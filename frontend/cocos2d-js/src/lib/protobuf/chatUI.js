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

            //var layerGradient = new cc.LayerGradient(cc.color(255, 0, 0), cc.color(0, 0, 255));
            //this.addChild(layerGradient, 0);
            var layerColor = new cc.LayerColor(cc.color('#202020'));
            this.addChild(layerColor, 0);

            this.addChild(new ChatLayer());
        }
    });

    var ChatLayer = cc.Layer.extend({

        chatApp:null,

        room:null,

        textCount:0,
        row:20,
        rowHeight:25,
        bottomPadding:10,
        chatScrollView:null,

        roomScrollView:null,
        roomList:[],

        textField:null,

        ctor:function () {
            this._super();

            var size = cc.winSize;

            var room = this.room = new ccui.Text("--- Room ---", "Marker Felt", 30);
            room.x = size.width/2;
            room.y = size.height - room.height;
            this.addChild(room);

            // Create the layout
            var layout = new ccui.Layout();
            layout.setContentSize(cc.size(650, 400));
            var layoutRect = layout.getContentSize();
            layout.x = size.width/2 - layoutRect.width/2;
            layout.y = size.height/2 - layoutRect.height/2 + 50;
            this.addChild(layout);

            var chatPanel = new ccui.Layout();
            chatPanel.setBackGroundColorType(ccui.Layout.BG_COLOR_SOLID);
            chatPanel.setBackGroundColor(cc.color(128, 128, 128));
            chatPanel.setContentSize(cc.size(512, 400));
            layout.addChild(chatPanel);

            //var hello = new cc.Sprite(res.HelloWorld_png);
            //layout.addChild(hello);

            //var layerColor = new cc.LayerColor(cc.color(0, 0, 0));
            //layout.addChild(layerColor);

            // Create the scrollview
            var chatScrollView = this.chatScrollView = new ccui.ScrollView();
            chatScrollView.setDirection(ccui.ScrollView.DIR_VERTICAL);
            chatScrollView.setTouchEnabled(true);
            chatScrollView.setBounceEnabled(true);
            chatScrollView.setContentSize(cc.size(512, 400));
            chatScrollView.x = 10;
            //chatScrollView.setAnchorPoint(cc.p(0.5,0.5));

            var innerWidth = chatScrollView.width;
            var innerHeight = this.row*this.rowHeight + this.row*this.bottomPadding;
            chatScrollView.setInnerContainerSize(cc.size(innerWidth, innerHeight));
            chatPanel.addChild(chatScrollView);

            var roomPanel = new ccui.Layout();
            roomPanel.setBackGroundColorType(chatPanel.getBackGroundColorType());
            roomPanel.setBackGroundColor(chatPanel.getBackGroundColor());
            roomPanel.setContentSize(cc.size(100, chatPanel.getContentSize().height));
            var layoutRect = chatPanel.getContentSize();
            roomPanel.x = chatPanel.x + chatPanel.width + 20;
            roomPanel.y = chatPanel.y;
            layout.addChild(roomPanel);

            var roomScrollView = this.roomScrollView = new ccui.ScrollView();
            roomScrollView.setDirection(ccui.ScrollView.DIR_VERTICAL);
            roomScrollView.setTouchEnabled(true);
            roomScrollView.setBounceEnabled(true);
            roomScrollView.setContentSize(cc.size(512, 400));
            roomScrollView.x = 5;

            var innerWidth2 = roomScrollView.width;
            var innerHeight2 = this.row*this.rowHeight + this.row*this.bottomPadding;
            roomScrollView.setInnerContainerSize(cc.size(innerWidth2, innerHeight2));
            //cc.log("i="+i);
            //text = new ccui.Text(i, "Thonburi", 20);
            //text.color = cc.color(255, 255, 0);
            //text.anchorX = 0;
            //text.x = 0;
            //text.y = roomScrollView.getInnerContainerSize().height - (this.rowHeight+this.bottomPadding);
            //roomScrollView.addChild(text);
            roomPanel.addChild(roomScrollView);

            // Create the textfield
            var textField = this.textField = new ccui.TextField("PlaceHolder", "Marker Felt", 30);
            textField.x = size.width / 2;
            textField.y = 100;
            textField.addEventListener(this.textFieldEvent, this);
            this.addChild(textField, 1);

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
                self.appendMessage(message);
            });

            socket.on(RESULT.joinResult, function (result) {
                result = ChatProtocolBuffer.JoinResultProto.decode(result);
                self.room.setString(result.room);
                self.appendMessage('Room changed!');
            });

            socket.on(MSG.message, function (message) {
                message = ChatProtocolBuffer.MessageProto.decode(message);
                self.appendMessage(message.text);
            });

            socket.on(MSG.rooms, function (rooms) {
                console.log(rooms);
                rooms = ChatProtocolBuffer.RoomsProto.decode(rooms).rooms;
                console.log(rooms);

                self.roomList = [];
                self.roomScrollView.removeAllChildren();

                for (var index in rooms) {
                    //room = room.substring(1, room.length);
                    if (index != '') {
                        self.appendRoom(rooms[index]);
                    }
                }

                /*$('#roomList div').click(function () {
                    chatApp.processCommand('/join ' + $(this).text())
                    $sendMessage.focus();
                });*/
            });

            this.schedule(function () {
                socket.emit(MSG.rooms);
            }, 5, cc.REPEAT_FOREVER, 1);
        },

        appendRoom:function(room){
            this.roomList.push(room);
            var text = new ccui.Text(room, "Thonburi", 20);
            text.color = cc.color(255, 255, 0);
            text.anchorX = 0;
            text.x = 0;
            text.y = this.roomScrollView.getInnerContainerSize().height - (this.rowHeight+this.bottomPadding)*(this.roomList.length);
            this.roomScrollView.addChild(text);

            //var percent = (this.textCount-1)/this.row;
            //cc.log(percent);
            /*if(percent == 0 || percent > 0.5){
                this.roomScrollView.jumpToPercentVertical((percent+0.01) * 100);
            }*/
        },

        appendMessage:function(message){
            if(this.textCount >= this.row-1){
                this.chatScrollView.removeAllChildren();
                this.textCount = 0;
            }

            this.textCount++;
            var text = new ccui.Text(message, "Thonburi", 20);
            text.color = cc.color(255, 255, 0);
            text.anchorX = 0;
            text.x = 0;
            text.y = this.chatScrollView.getInnerContainerSize().height - (this.rowHeight+this.bottomPadding)*(this.textCount);
            this.chatScrollView.addChild(text);

            var percent = (this.textCount-1)/this.row;
            //cc.log(percent);
            if(percent == 0 || percent > 0.5){
                this.chatScrollView.jumpToPercentVertical((percent+0.01) * 100);
            }
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
                systemMessage = this.chatApp.processCommand(message);
                if (systemMessage) {
                    this.appendMessage(systemMessage);
                }
            } else {
                this.chatApp.sendMessage(this.room.getString(), message);
                this.appendMessage(message);
                //$messages.scrollTop($messages.prop('scrollHeight'));
            }

            this.textField.setString('');
            //this.chatScrollView.updateChildren();
        }

    });

    return {
        ChatScene:ChatScene,
        ChatLayer:ChatLayer
    };

});