require.config({
    baseUrl: 'static/js/lib',
    paths: {
        jquery:'jquery-1.8.3.min',
        socketio:'socket.io/socket.io',
        protocol:'../protocol',
        chat:'../chat',
        chatUI:'../chatUI',
    }
});

require(['jquery', 'socketio', 'protocol', 'chat', 'chatUI'], function($, socketio, protocol){
    console.log("requireJS end...");
    //console.log(protocol.MSG.connection);
});