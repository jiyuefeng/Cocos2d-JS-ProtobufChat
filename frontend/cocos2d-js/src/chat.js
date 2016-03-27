//var MSG = require('../../../../backend/nodejs/lib/chatServer.js').MSG;
define(['protocol', 'ProtoBuf'], function(protocol, ProtoBuf){

    var ChatProtocolBuffer = ProtoBuf.loadProtoFile("src/lib/protobuf/ChatProtoBuf.proto")
        .build("ChatProtocolBuffer");
    cc.log('chat, '+ChatProtocolBuffer);

    var MSG = protocol.MSG;

    var Chat = function (socket) {
        this.socket = socket;
    };

    Chat.prototype.sendMessage = function (room, text) {
        //var message = {
        //    room: room,
        //    text: text,
        //};
        this.socket.emit(MSG.message, new ChatProtocolBuffer.MessageProto({
            room: room,
            text:text
        }).toBuffer());
    };

    Chat.prototype.processCommand = function (command) {
        var words = command.split(' ');
        var command = words[0].substring(1, words[0].length).toLowerCase();
        var message = false;
        switch (command) {
            case 'join':
                words.shift();
                var room = words.join(' ');
                this.socket.emit(MSG.join, {newRoom: room,});
                break;
            case 'name':
                words.shift();
                var userName = words.join(' ');
                this.socket.emit(MSG.changeName, userName);
                break;
            default:
                message = 'Unknow Command:' + command;
                break;
        }
    }

    return {
      Chat:Chat
    };
});