define(function(){
    var MSG = {
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

    console.log('define protocol end..');

    return {
        MSG:MSG,
        RESULT:RESULT
    };
});