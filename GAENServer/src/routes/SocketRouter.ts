import { Connection } from "typeorm";
import { Config } from "../entity/Config"

module.exports = function(socket, socketDict, app) {
    const socketId = socket.id;
    var consoleId = socketDict['console'];
    var deviceName = "";

    console.log('Socket connected.');
    // Ask for client type
    socket.emit('client-type');
    console.log('Emitted client-type');
    console.log(socketDict);

    socket.on('test', function(data) {
        console.log('test function called.');
    })

    // Check client type (device/console)
    socket.on('type-console', async function(data) {
        var idx = socketDict['console'];
        if (idx == undefined) {
            socketDict['console'] = socketId;
            consoleId = socketId;
            deviceName = 'console';
            let db: Connection = app.get('db');
            let config: Config;
            try {
                config = await db.manager.getRepository(Config).findOne();
            } catch (e) {
                console.log("Failed to fetch config. Exception:");
                console.log(e);
                socket.emit('init-console-fail');
                return;
            }
            socket.emit('init-console', config);
        } else {
            socket.emit('refuse-console');
        }
        console.log(socketDict);
    })

    socket.on('type-device', function(data) {
        console.log("type-device called");
        var idx = socketDict[data.deviceName];
        console.log(idx);
        if (idx == undefined) {
            socketDict[data.deviceName] = socketId;
            socket.emit('init-device');
            deviceName = data.deviceName;
        } else {
            socket.emit('refuse-device');
        }
        console.log(socketDict);
    })

    // Start/stop functions
    socket.on('start', function(data) {
        console.log('start called');
        const testId = data.testId;
        console.log(testId);
        socket.broadcast.emit('start', {
            testId: testId
        });
        socket.emit('start-done');
    })

    socket.on('stop', function(data) {
        console.log('stop called');
        socket.broadcast.emit('stop');
        socket.emit('stop-done');
    })

    // Remove socket from dictionay on disconnect
    socket.on('disconnect', function(data) {
        console.log('Socket disconnected.');
        delete socketDict[deviceName];
        console.log(socketDict);
    })
}

function removeDev(socketList, deviceList, devName) {
    const index = deviceList.indexOf(devName);
    if (index > -1) {
        socketList.splice(index, 1);
        deviceList.splice(index, 1);
    }
}