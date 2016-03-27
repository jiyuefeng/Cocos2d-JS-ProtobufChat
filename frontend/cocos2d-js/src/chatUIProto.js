define(['socketio', 'protocol', 'chat', 'ByteBuffer', 'Long', 'ProtoBuf'], function(socketio, protocol, chat, ByteBuffer, Long, ProtoBuf){

    //console.log(ProtoBuf);
    var ChatProtocolBuffer = ProtoBuf.loadProtoFile("src/lib/protobuf/ChatProtoBuf.proto")
            .build("ChatProtocolBuffer"),
        TestProto = ChatProtocolBuffer.TestProto;
    //console.log(TestProto);

    var MSG = protocol.MSG;
    var RESULT = protocol.RESULT;

    var ChatScene = cc.Scene.extend({
        onEnter:function () {
            this._super();

            var layerGradient = new cc.LayerGradient(cc.color(255, 0, 0), cc.color(0, 0, 255));
            cc.log("cc.winSize="+cc.winSize);
            this.addChild(layerGradient, 0);

            this.addChild(new ChatLayer());
        }
    });

    var ChatLayer = cc.Layer.extend({

        chatApp:null,

        room:null,

        textCount:0,
        row:36,
        rowHeight:25,
        bottomPadding:10,
        scrollView:null,
        texts:[],

        textField:null,

        ctor:function () {
            this._super();

            var size = cc.winSize;

            // Create the scrollview
            var scrollView = this.scrollView = new ccui.ScrollView();
            scrollView.setDirection(ccui.ScrollView.DIR_VERTICAL);
            scrollView.setTouchEnabled(true);
            scrollView.setBounceEnabled(true);
            scrollView.setContentSize(cc.size(512, 400));
            scrollView.x = size.width/2;
            scrollView.y = size.height/1.7;
            scrollView.setAnchorPoint(cc.p(0.5,0.5));
            cc.log('_autoScroll='+scrollView._autoScroll);
            scrollView._autoScroll = true;
            cc.log('_autoScroll='+scrollView._autoScroll);

            var start = this.room = new ccui.Text("--- Room ---", "Marker Felt", 30);
            start.x = scrollView.x;
            start.y = size.height - start.height;
            this.addChild(start);

            var innerWidth = scrollView.width;
            //var row = 36;
            //var rowHeight = 25;
            //var bottomPadding = 10;
            var innerHeight = this.row*this.rowHeight + this.row*this.bottomPadding;
            scrollView.setInnerContainerSize(cc.size(innerWidth, innerHeight));

            /*for(var i = 0; i < this.row; i++){
                //var text = new cc.LabelTTF("This is a test label: " + i,"Microsoft YaHei",14,cc.TEXT_ALIGNMENT_LEFT);
                var text = new ccui.Text("This is a test 测试 label: " + i, "Thonburi", 20);
                text.color = cc.color(255, 255, 0);
                text.anchorX = 0;
                text.x = 0;
                text.y = scrollView.getInnerContainerSize().height - (this.rowHeight+this.bottomPadding)*(i+1);

                scrollView.addChild(text);
            }*/
            //scrollView.jumpToLeft();

            this.addChild(scrollView);

            // Create the textfield
            var textField = this.textField = new ccui.TextField("PlaceHolder", "Marker Felt", 30);
            textField.x = size.width / 2;
            textField.y = 100;
            textField.addEventListener(this.textFieldEvent, this);
            this.addChild(textField);

            this._init();

            return true;
        },

        _init:function(){
            var self = this;

            var socket = socketio.connect('localhost:3000');
            //console.log(socket);

            this.chatApp = new chat.Chat(socket);

            socket.on(RESULT.nameResult, function (result) {
                result = ChatProtocolBuffer.NameResultProto.decode(result);
                var message;
                if (result.success) {
                    message = 'You are now known as [' + result.name + ']!';
                } else {
                    message = result.message;
                }
                self.appendText(message);
            });

            socket.on(RESULT.joinResult, function (result) {
                result = ChatProtocolBuffer.JoinResultProto.decode(result);
                self.room.setString(result.room);
                self.appendText('Room changed!');
            });

            socket.on(MSG.message, function (message) {
                message = ChatProtocolBuffer.MessageProto.decode(message);
                self.appendText(message.text);
            });
        },

        appendText:function(message){
            this.textCount++;
            var text = new ccui.Text(message, "Thonburi", 20);
            text.color = cc.color(255, 255, 0);
            text.anchorX = 0;
            text.x = 0;
            text.y = this.scrollView.getInnerContainerSize().height - (this.rowHeight+this.bottomPadding)*(this.textCount);
            this.scrollView.addChild(text);

            /*this.texts.push(text);
            if(this.texts.length > 5){
                this.scrollView.removeAllChildren();
                this.texts = [];
                //this.scrollView.removeChild(this.texts.shift());
            }*/

            this.scrollView.jumpToPercentVertical(5);
        },

        textFieldEvent: function (textField, type) {
            var widgetSize = cc.winSize;
            switch (type) {
                case ccui.TextField.EVENT_ATTACH_WITH_IME:
                    textField.runAction(cc.moveTo(0.225, cc.p(textField.x, textField.y + 30)));
                    cc.log("attach with IME");
                    break;
                case ccui.TextField.EVENT_DETACH_WITH_IME:
                    textField.runAction(cc.moveTo(0.175, cc.p(textField.x, textField.y - 30)));
                    cc.log("detach with IME");
                    this.processUserInput();
                    break;
                case ccui.TextField.EVENT_INSERT_TEXT:
                    //cc.log("insert words:"+textField.getString());
                    break;
                case ccui.TextField.EVENT_DELETE_BACKWARD:
                    cc.log("delete word");
                    break;
                default:
                    break;
            }
        },

        processUserInput:function() {
            //var $messages = $('#messages');
            var message = this.textField.getString();
            var systemMessage;

            if (message.charAt(0) == '/') {
                systemMessage = chatApp.processCommand(message);
                if (systemMessage) {
                    this.appendText(systemMessage);
                }
            } else {
                this.chatApp.sendMessage(this.room.getString(), message);
                this.appendText(message);
                //$messages.scrollTop($messages.prop('scrollHeight'));
            }

            this.textField.setString('');
            //this.scrollView.updateChildren();
        }

    });

    return {
        ChatScene:ChatScene,
        ChatLayer:ChatLayer
    };

});