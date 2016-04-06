require.config({
    paths: {
        jquery:'lib/jquery-1.8.3.min',
        socketio:'lib/socket.io/socket.io',
        protocol:'protocol',
        ByteBuffer:'lib/protobuf/ByteBufferAB',
        Long:'lib/protobuf/Long',
        ProtoBuf:'lib/protobuf/ProtoBuf',
        chat:'lib/[chatType]/chat',
        chatUI:'lib/[chatType]/chatUI',
    }
});

require(['jquery', 'socketio', 'protocol', 'chat', 'chatUI'],
    function($, socketio, protocol, chat, chatUI){
        console.log("requireJS end...");
        //console.log(protocol.MSG.connection);
    }
);