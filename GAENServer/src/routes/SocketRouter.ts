import { Connection } from "typeorm";
import { Config } from "../entity/Config"

module.exports = function(socket, socketDict, io, app) {
    const socketId = socket.id;
    var consoleId = socketDict['console'];

    console.log('Socket connected.');
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
        } else {
            socket.emit('refuse-device');
        }
    })

    socket.on('disconnect', function(data) {
        console.log('Socket disconnected.');
        if (consoleId == socketId) {
            delete socketDict['console'];
        }
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