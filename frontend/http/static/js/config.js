require.config({
    paths: {
        jquery:'lib/jquery-1.8.3.min',
        socketio:'lib/socket.io/socket.io',
        protocol:'protocol',
        chat:'chat',
        chatUI:'chatUI',
    }
});

require(['jquery', 'socketio', 'protocol', 'chat', 'chatUI'],
    function($, socketio, protocol, chat, chatUI){
        console.log("requireJS end...");
        //console.log(protocol.MSG.connection);
    }
);