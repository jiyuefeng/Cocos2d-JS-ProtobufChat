var ChatLayer = cc.Layer.extend({

    ctor:function () {
        this._super();

        var size = cc.winSize;

        // Create the scrollview
        var scrollView = new ccui.ScrollView();
        scrollView.setDirection(ccui.ScrollView.DIR_VERTICAL);
        scrollView.setTouchEnabled(true);
        scrollView.setBounceEnabled(true);
        scrollView.setContentSize(cc.size(512, 400));
        scrollView.x = size.width/2;
        scrollView.y = size.height/1.7;
        scrollView.setAnchorPoint(cc.p(0.5,0.5));

        var start = new ccui.Text("--- start ---", "Marker Felt", 30);
        start.x = scrollView.x;
        start.y = size.height - start.height;
        this.addChild(start);

        var innerWidth = scrollView.width;
        var row = 36;
        var rowHeight = 25;
        var bottomPadding = 10;
        var innerHeight = row*rowHeight + row*bottomPadding;
        scrollView.setInnerContainerSize(cc.size(innerWidth, innerHeight));

        for(var i = 0; i < row; i++){
            //var text = new cc.LabelTTF("This is a test label: " + i,"Microsoft YaHei",14,cc.TEXT_ALIGNMENT_LEFT);
            var text = new ccui.Text("This is a test 测试 label: " + i, "Thonburi", 20);
            text.color = cc.color(255, 255, 0);
            text.anchorX = 0;
            text.x = 0;
            text.y = scrollView.getInnerContainerSize().height - (rowHeight+bottomPadding)*(i+1);

            scrollView.addChild(text);
        }
        //scrollView.jumpToLeft();

        this.addChild(scrollView);

        // Create the textfield
        var textField = new ccui.TextField("PlaceHolder", "Marker Felt", 30);
        textField.x = size.width / 2;
        textField.y = 100;
        textField.addEventListener(this.textFieldEvent, this);
        this.addChild(textField);

        return true;
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
                break;
            case ccui.TextField.EVENT_INSERT_TEXT:
                cc.log("insert words");
                break;
            case ccui.TextField.EVENT_DELETE_BACKWARD:
                cc.log("delete word");
                break;
            default:
                break;
        }
    }

});