var protocol = require('./protocol');
var MSG = protocol.MSG;
var RESULT = protocol.RESULT;

var socketio = require('socket.io');
var io;
var guestNum = 1;
var userNames = {};
var namesUsed = [];
var currentRoom = {};
var existsRooms = {};

var USER_NAME_PREFIX = 'Guest';
var DEFAULT_ROOM = 'Lobby';

/*var MSG = {
    connection:'connection',
    message:'message',
    changeName:'changeName',
    join:'join',
    rooms:'rooms',
    disconnect:'disconnect',
};

var RESULT = {
    nameResult:'nameResult',
    joinResult:'joinResult',
}

exports.MSG = MSG;
exports.RESULT = RESULT;
 */

exports.listen = function(server){
    io = socketio.listen(server);
    io.set('log level', 1);

    //console.log(io.sockets);

    io.sockets.on(MSG.connection, function(socket){
        //console.log(io.sockets);
        console.log(socket.id+' connecting...');

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
    socket.emit(RESULT.nameResult, {
        success:true,
        name:userName
    });
    namesUsed.push(userName);
    return guestNum+1;
}

function joinRoom(socket, room){
    if(!existsRooms[room]){
        existsRooms[room] = {};
        existsRooms[room][socket.id] = socket.id;
        existsRooms[room].length = 1;
    }else{
        existsRooms[room][socket.id] = socket.id;
        existsRooms[room].length++;
    }

    socket.join(room);
    currentRoom[socket.id] = room;
    socket.emit(RESULT.joinResult, {room:room});
    socket.broadcast.to(room).emit(MSG.message, {
        text:userNames[socket.id]+' has joined '+room+'!'
    });

    //console.log(socket.id+' '+room);
    //console.log(io);
    //console.log("\n\n\n");
    //console.log(io.sockets);

    socket.emit(MSG.message, {text:usersInRoomSummary(room)});
}

function usersInRoomSummary(room){
    //var usersInRoom = io.sockets.clients(room);
    //var usersInRoom = io.sockets.adapter.rooms[room];
    var usersInRoom = existsRooms[room];
    if(usersInRoom.length > 0){
        var usersInRoomSummary = 'Users currently in '+room+': ';
        var index = 0;
        for(var key in usersInRoom){
            if(key == 'length'){
                continue;
            }
            var userSocketId = usersInRoom[key]/*.id*/;
            if(index > 0){
                usersInRoomSummary += ', ';
            }
            usersInRoomSummary += userNames[userSocketId];
            index++;
        }
    }
    usersInRoomSummary += '!';
    return usersInRoomSummary;
}

function handleBroadcastMessage(socket, userNames){
    socket.on(MSG.message, function(message){
        socket.broadcast.to(message.room).emit(MSG.message, {
            text:userNames[socket.id]+': '+message.text
        });
    });
}

function handleChangeUserName(socket, userNames, namesUsed){
    socket.on(MSG.changeName, function(userName){
        if(userName.indexOf(USER_NAME_PREFIX) == 0){
            socket.emit(RESULT.nameResult, {
                success:false,
                message:'Names cannot begin with '+USER_NAME_PREFIX+'!'
            });
        }else{
            if(namesUsed.indexOf(userName) == -1){
                var preUserName = userNames[socket.id];
                var preUserNameIndex = namesUsed.indexOf(preUserName);
                namesUsed.push(userName);
                userNames[socket.id] = userName;
                delete namesUsed[preUserNameIndex];

                socket.emit(RESULT.nameResult, {
                    success:true,
                    name:userName
                });
                socket.broadcast.to(currentRoom[socket.id]).emit(MSG.message, {
                    text:preUserName+' is now known as ['+userName+']!'
                });
            }else{
                socket.emit(RESULT.nameResult, {
                    success:false,
                    message:'That name is already in use!'
                });
            }
        }
    });
}

function handleJoinOtherRoom(socket){
    socket.on(MSG.join, function(joinInfo){
        var preRoom = currentRoom[socket.id];
        socket.leave(preRoom);
        socket.broadcast.to(preRoom).emit(MSG.message, {
            text:userNames[socket.id]+' changed room to ['+joinInfo.newRoom+']!'
        });
        socket.broadcast.to(preRoom).emit(MSG.message, {
            text:usersInRoomSummary(preRoom)
        });

        deleteFromExistsRooms(socket);

        joinRoom(socket, joinInfo.newRoom);
    });
}

function handleQueryRooms(socket){
    socket.on(MSG.rooms, function(){
        socket.emit(MSG.rooms, existsRooms);
    });
}

function handleDisconnect(socket, userNames, namesUsed){
    socket.on(MSG.disconnect, function(){
        var userNameIndex = namesUsed.indexOf(userNames[socket.id]);
        delete namesUsed[userNameIndex];
        delete userNames[socket.id];

        deleteFromExistsRooms(socket);
    });
}

function deleteFromExistsRooms(socket){
    delete existsRooms[currentRoom[socket.id]][socket.id];
    if(--existsRooms[currentRoom[socket.id]].length <= 0){
        delete existsRooms[currentRoom[socket.id]];
    }
}