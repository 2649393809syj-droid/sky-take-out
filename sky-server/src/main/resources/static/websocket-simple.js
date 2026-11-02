/**
 * WebSocket 简单集成方案
 * 直接复制这段代码到你的前端项目中使用
 */

// 全局 WebSocket 对象
let websocket = null;

/**
 * 连接 WebSocket
 * 自动根据当前页面的协议和域名生成 WebSocket 地址
 */
function connectWebSocket() {
    if (websocket && websocket.readyState === WebSocket.OPEN) {
        console.log("WebSocket已连接");
        return;
    }
    
    // 自动获取 WebSocket 地址（不需要指定端口）
    const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsHost = window.location.host; // 自动包含端口号
    const wsUrl = `${wsProtocol}//${wsHost}/ws/admin`;
    
    console.log("正在连接 WebSocket:", wsUrl);
    
    websocket = new WebSocket(wsUrl);
    
    websocket.onopen = function() {
        console.log("✓ WebSocket连接成功");
    };
    
    websocket.onmessage = function(event) {
        console.log("收到消息：", event.data);
        
        try {
            const data = JSON.parse(event.data);
            
            if (data.type === 1) {
                // 新订单通知
                handleNewOrder(data);
            } else if (data.type === 2) {
                // 客户催单
                handleReminder(data);
            }
        } catch (e) {
            console.error("解析消息失败", e);
        }
    };
    
    websocket.onclose = function() {
        console.log("WebSocket连接关闭，5秒后重连");
        websocket = null;
        setTimeout(connectWebSocket, 5000);
    };
    
    websocket.onerror = function(error) {
        console.error("WebSocket错误", error);
    };
}

/**
 * 处理新订单通知
 */
function handleNewOrder(data) {
    console.log("新订单：", data);
    
    // 1. 显示通知（Element UI）
    if (typeof ElementUI !== 'undefined' && ElementUI.Notification) {
        ElementUI.Notification({
            title: '新订单提醒',
            message: data.content,
            type: 'success',
            duration: 0,
            position: 'top-right'
        });
    }
    // 或者使用 Vue 实例的 $notify
    else if (typeof Vue !== 'undefined' && window.vm && window.vm.$notify) {
        window.vm.$notify({
            title: '新订单提醒',
            message: data.content,
            type: 'success',
            duration: 0,
            position: 'top-right'
        });
    }
    // 浏览器原生通知
    else if (Notification.permission === "granted") {
        new Notification("新订单提醒", {
            body: data.content,
            icon: '/logo.png'
        });
    }
    
    // 2. 播放提示音
    playSound('/audio/preview.mp3');
    
    // 3. 语音播报
    playVoice('您有新的订单，请及时处理');
}

/**
 * 处理催单通知
 */
function handleReminder(data) {
    console.log("客户催单：", data);
    
    if (typeof ElementUI !== 'undefined' && ElementUI.Notification) {
        ElementUI.Notification({
            title: '客户催单',
            message: data.content,
            type: 'warning',
            duration: 0,
            position: 'top-right'
        });
    } else if (typeof Vue !== 'undefined' && window.vm && window.vm.$notify) {
        window.vm.$notify({
            title: '客户催单',
            message: data.content,
            type: 'warning',
            duration: 0,
            position: 'top-right'
        });
    }
    
    playSound('/audio/reminder.mp3');
    playVoice('有客户催单，请尽快处理');
}

/**
 * 播放提示音
 */
function playSound(audioUrl) {
    try {
        const audio = new Audio(audioUrl);
        audio.play().catch(e => {
            console.log('播放提示音失败（可能需要用户交互）', e);
        });
    } catch (e) {
        console.error('播放提示音错误', e);
    }
}

/**
 * 语音播报
 */
function playVoice(text) {
    if ('speechSynthesis' in window) {
        try {
            // 停止当前播报
            window.speechSynthesis.cancel();
            
            const utterance = new SpeechSynthesisUtterance(text);
            utterance.lang = 'zh-CN';
            utterance.rate = 1.0;
            utterance.pitch = 1.0;
            utterance.volume = 1.0;
            
            window.speechSynthesis.speak(utterance);
        } catch (e) {
            console.error('语音播报错误', e);
        }
    } else {
        console.log('浏览器不支持语音播报');
    }
}

/**
 * 初始化（页面加载时调用）
 */
function initWebSocket() {
    // 请求通知权限
    if ('Notification' in window && Notification.permission === 'default') {
        Notification.requestPermission().then(permission => {
            console.log('通知权限：', permission);
        });
    }
    
    // 连接 WebSocket
    connectWebSocket();
}

/**
 * 清理（页面卸载时调用）
 */
function cleanupWebSocket() {
    if (websocket) {
        websocket.close();
        websocket = null;
    }
}

// 自动初始化（如果页面已加载）
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initWebSocket);
} else {
    initWebSocket();
}

// 页面卸载时清理
window.addEventListener('beforeunload', cleanupWebSocket);

// 导出函数供外部使用
if (typeof module !== 'undefined' && module.exports) {
    module.exports = {
        connectWebSocket,
        initWebSocket,
        cleanupWebSocket
    };
}
