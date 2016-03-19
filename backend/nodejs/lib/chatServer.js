var socketio = require("socket.io");
var io;
var guestNum = 1;
var userNames = {};
var namesUsed = [];
var currentRoom = {};

var USER_NAME_PREFIX = "Guest";
var DEFAULT_ROOM = "Lobby";

exports.listen = function(server){
    io = socketio.listen(server);
    io.set("log level", 1);

    io.sockets.on("connection", function(socket){
        guestNum = assignGuestName(socket, guestNum, userNames, namesUsed);
        joinRoom(socket, DEFAULT_ROOM);

        handleBroadcastMessage(socket, userNames);
        handleChangeUserName(socket, userNames, namesUsed);
        handleJoinOtherRoom(socket);
        handleQueryRooms(socket);
        handleDisconnect(socket, userNames, namesUsed);
    });
}

function assignGuestName(socket, guestNum, userNames, namesUsed){
    var userName = USER_NAME_PREFIX+guestNum;
    userNames[socket.id] = userName;
    socket.emit("nameResult", {
        success:true,
        name:userName
    });
    namesUsed.push(userName);
    return guestNum+1;
}

function joinRoom(socket, room){
    socket.join(room);
    currentRoom[socket.id] = room;
    socket.emit("joinResult", {room:room});
    socket.broadcast.to(room).emit("message", {
        text:userNames[socket.id]+" has joined "+room+"!"
    });

    var usersInRoom = io.socket.clients(room);
    if(usersInRoom.length > 0){
        var usersInRoomSummary = "Users currently in "+room+": ";
        for(var index in usersInRoom){
            var userSocketId = usersInRoom[index].id;
            if(userSocketId != socket.id){
                if(index > 0){
                    usersInRoomSummary += ", ";
                }
                usersInRoomSummary += userNames[userSocketId];
            }
        }
    }
    usersInRoomSummary += "!";
    socket.emit("message", {text:usersInRoomSummary});
}

function handleBroadcastMessage(socket, userNames){
    socket.on("message", function(message){
        socket.broadcast.to(message.room).emit("message", {
            text:userNames[socket.id]+": "+message.text
        });
    });
}

function handleChangeUserName(socket, userNames, namesUsed){
    socket.on("changeName", function(userName){
        if(userName.indexOf(USER_NAME_PREFIX) == 0){
            socket.emit("nameResult", {
                success:false,
                message:"Names cannot begin with "+USER_NAME_PREFIX+"!"
            });
        }else{
            if(namesUsed.indexOf(userName) == -1){
                var preUserName = userNames[socket.id];
                var preUserNameIndex = namesUsed.indexOf(preUserName);
                namesUsed.push(userName);
                userNames[socket.id] = userName;
                delete namesUsed[preUserNameIndex];

                socket.emit("nameResult", {
                    success:true,
                    name:userName
                });
                socket.broadcast.to(currentRoom[socket.id]).emit("message", {
                    text:preUserName+" is now known as '"+userName+"'!"
                });
            }else{
                socket.emit("nameResult", {
                    success:false,
                    message:"That name is already in use!"
                });
            }
        }
    });
}

function handleJoinOtherRoom(socket){
    socket.on("join", function(joinInfo){
        socket.leave(currentRoom[socket.id]);
        joinRoom(socket, joinInfo.newroom);
    });
}

function handleQueryRooms(socket){
    socket.on("rooms", function(){
        socket.emit("rooms", io.socket.manager.rooms);
    });
}

function handleDisconnect(socket, userNames, namesUsed){
    socket.on("disconnect", function(){
        var userNameIndex = namesUsed.indexOf(userNames[socket.id]);
        delete namesUsed[userNameIndex];
        delete userNames[socket.id];
    });
}