//var chatServer = require('../../../../backend/nodejs/lib/chatServer.js');
//var MSG = chatServer.MSG;
//var RESULT = chatServer.RESULT;

define(['jquery', 'socketio', 'protocol', 'chat', 'ByteBuffer', 'Long', 'ProtoBuf'], function($, socketio, protocol, chat, ByteBuffer, Long, ProtoBuf) {
    console.log(ProtoBuf);
    var ChatProtocolBuffer = ProtoBuf.loadProtoFile("static/js/lib/protobuf/ChatProtoBuf.proto")
            .build("ChatProtocolBuffer"),
        TestProto = ChatProtocolBuffer.TestProto;
    console.log(TestProto);

    var MSG = protocol.MSG;
    var RESULT = protocol.RESULT;

    var socket = socketio.connect();
    console.log(socket);

    $(document).ready(function () {
        var $messages = $('#messages');
        var $roomList = $('#roomList');
        var $sendMessage = $('#sendMessage');
        var chatApp = new chat.Chat(socket);
        console.log('jquery ready...');

        socket.on(RESULT.nameResult, function (result) {
            var message;
            if (result.success) {
                message = 'You are now known as [' + result.name + ']!';
            } else {
                message = result.message;
            }
            $messages.append(divSystemContentElement(message));
        });

        socket.on(RESULT.joinResult, function (result) {
            $('#room').text(result.room);
            $messages.append(divSystemContentElement('Room changed!'));
        });

        socket.on(MSG.message, function (message) {
            $messages.append($('<div></div>').text(message.text));
        });

        socket.on(MSG.rooms, function (rooms) {
            var roomsMsg = TestProto.decode(rooms);
            console.log(roomsMsg);
            console.log(roomsMsg.gold);

            $roomList.empty();

            for (var room in rooms) {
                //room = room.substring(1, room.length);
                if (room != '') {
                    $roomList.append(divEscapedContentElement(room));
                }
            }

            $('#roomList div').click(function () {
                chatApp.processCommand('/join ' + $(this).text())
                $sendMessage.focus();
            });
        });

        setInterval(function () {
            socket.emit(MSG.rooms);
        }, 5000);

        $sendMessage.focus();

        $('#sendForm').submit(function () {
            processUserInput(chatApp, socket);
            return false;
        });
    });

    function divEscapedContentElement(message) {
        return $('<div></div>').text(message);
    }

    function divSystemContentElement(message) {
        return $('<div></div>').html('<i>' + message + '</i>');
    }

    function processUserInput(chatApp, socket) {
        var $messages = $('#messages');
        var $sendMessage = $('#sendMessage');
        var message = $sendMessage.val();
        var systemMessage;

        if (message.charAt(0) == '/') {
            systemMessage = chatApp.processCommand(message);
            if (systemMessage) {
                $messages.append(divSystemContentElement(systemMessage));
            }
        } else {
            chatApp.sendMessage($('#room').text(), message);
            $messages.append(divEscapedContentElement(message));
            $messages.scrollTop($messages.prop('scrollHeight'));
        }

        $sendMessage.val('');
    }
});