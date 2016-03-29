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
        scrollView:null,

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
            scrollView.x = size.width/2 - 60;
            scrollView.y = size.height/1.7;
            scrollView.setAnchorPoint(cc.p(0.5,0.5));
            cc.log('_autoScroll='+scrollView._autoScroll);
            scrollView._autoScroll = true;
            cc.log('_autoScroll='+scrollView._autoScroll);

            var start = this.room = new ccui.Text("--- Room ---", "Marker Felt", 30);
            start.x = size.width/2;
            start.y = size.height - start.height;
            this.addChild(start, 1);

            var innerWidth = scrollView.width;
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

            this.addChild(scrollView, 1);

            // Create the textfield
            var textField = this.textField = new ccui.TextField("PlaceHolder", "Marker Felt", 30);
            textField.x = size.width / 2;
            textField.y = 100;
            textField.addEventListener(this.textFieldEvent, this);
            this.addChild(textField, 1);

            // Create the layout
            var layout = new ccui.Layout();
            layout.setBackGroundColorType(ccui.Layout.BG_COLOR_SOLID);
            layout.setBackGroundColor(cc.color(128, 128, 128));
            var scrollViewSize = scrollView.getContentSize();
            layout.setContentSize(cc.size(scrollViewSize.width+20, scrollViewSize.height+10));
            var layoutRect = layout.getContentSize();
            layout.x = scrollView.x - layoutRect.width/ 2;
            layout.y = scrollView.y - layoutRect.height / 2;
            this.addChild(layout, 0);

            var layout2 = new ccui.Layout();
            layout2.setBackGroundColorType(layout.getBackGroundColorType());
            layout2.setBackGroundColor(layout.getBackGroundColor());
            layout2.setContentSize(cc.size(100, layout.getContentSize().height));
            var layoutRect = layout.getContentSize();
            layout2.x = layout.x + layoutRect.width + 20;
            layout2.y = layout.y;
            this.addChild(layout2, 0);

            var scrollView2 = new ccui.ScrollView();
            scrollView2.setDirection(ccui.ScrollView.DIR_VERTICAL);
            scrollView2.setTouchEnabled(true);
            scrollView2.setBounceEnabled(true);
            scrollView2.setContentSize(cc.size(512, 400));
            //scrollView2.x = size.width/2 - 60;
            //scrollView2.y = size.height/1.7;
            scrollView2.setAnchorPoint(cc.p(0.5,0.5));

            var innerWidth2 = scrollView2.width;
            var innerHeight2 = this.row*this.rowHeight + this.row*this.bottomPadding;
            scrollView2.setInnerContainerSize(cc.size(innerWidth2, innerHeight2));
            cc.log("i="+i);
            text = new ccui.Text(i, "Thonburi", 20);
            text.color = cc.color(255, 255, 0);
            text.anchorX = 0;
            text.x = 0;
            text.y = scrollView2.getInnerContainerSize().height - (this.rowHeight+this.bottomPadding);
            scrollView2.addChild(text);
            layout2.addChild(scrollView2);

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
            if(this.textCount >= this.row-1){
                this.scrollView.removeAllChildren();
                this.textCount = 0;
            }

            this.textCount++;
            var text = new ccui.Text(message, "Thonburi", 20);
            text.color = cc.color(255, 255, 0);
            text.anchorX = 0;
            text.x = 0;
            text.y = this.scrollView.getInnerContainerSize().height - (this.rowHeight+this.bottomPadding)*(this.textCount);
            this.scrollView.addChild(text);

            var percent = (this.textCount-1)/this.row;
            //cc.log(percent);
            if(percent == 0 || percent > 0.5){
                this.scrollView.jumpToPercentVertical((percent+0.01) * 100);
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