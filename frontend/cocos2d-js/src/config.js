require.config({
    paths: {
        socketio:'src/lib/socket.io/socket.io',
        protocol:'src/protocol',
        ByteBuffer:'src/lib/protobuf/ByteBufferAB',
        Long:'src/lib/protobuf/Long',
        ProtoBuf:'src/lib/protobuf/ProtoBuf',
        chat:'src/lib/[chatType]/chat',
        chatUI:'src/lib/[chatType]/chatUI',
        //app:'src/app',
    }
});

require(['socketio', 'protocol', 'chat', 'chatUI'],
    function(socketio, protocol, chat, chatUI){
        console.log("requireJS end...");
        console.log(chatUI.ChatScene);
        //console.log(protocol.MSG.connection);
    }
);