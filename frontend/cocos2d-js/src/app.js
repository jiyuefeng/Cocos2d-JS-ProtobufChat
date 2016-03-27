//define(function(require){
//
//    var ChatScene = cc.Scene.extend({
//        onEnter:function () {
//            this._super();
//
//            var layerGradient = new cc.LayerGradient(cc.color(255, 0, 0), cc.color(0, 0, 255));
//            cc.log("cc.winSize="+cc.winSize);
//            this.addChild(layerGradient, 0);
//
//            var ChatLayer = require('chatUI');
//            this.addChild(new ChatLayer());
//        }
//    });
//
//    return ChatScene;
//
//});