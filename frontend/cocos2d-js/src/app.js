var ChatScene = cc.Scene.extend({
    onEnter:function () {
        this._super();

        var layerGradient = new cc.LayerGradient(cc.color(255, 0, 0), cc.color(0, 0, 255));
        this.addChild(layerGradient, 0);

        this.addChild(new ChatLayer());
    }
});

var ChatLayer = cc.Layer.extend({
    ctor:function () {
        this._super();

        var size = cc.winSize;

        var listView = new ccui.ScrollView();
        listView.setDirection(ccui.ScrollView.DIR_HORIZONTAL);
        listView.setTouchEnabled(true);
        listView.setBounceEnabled(true);
        listView.setSize(cc.size(512, 200));
        listView.x = size.width/2;
        listView.y = size.height/2;
        listView.setAnchorPoint(cc.p(0.5,0.5));
        this.addChild(listView);
        listView.setInnerContainerSize(cc.size(128*6, 200));
        for(var i =0; i < 6; i++){
            //var sprite = new cc.Sprite(res.HelloWorld_png);
            //listView.addChild(sprite);
            //sprite.x= i*130 + 40;
            //sprite.y= listView.getInnerContainerSize().height/2;sprite.setAnchorPoint(cc.p(0.5,0.5));

            var text = new cc.LabelTTF(i,"Microsoft YaHei",14,cc.TEXT_ALIGNMENT_LEFT);
            text.color = cc.color(255,255,0);
            text.anchorX = 0;
            text.x = 0;
            text.y = listView.getInnerContainerSize().height-25*(i+1);

            listView.addChild(text);
        }


        listView.jumpToLeft();
        cc.log("ben guo...");

        return true;
    }
});

