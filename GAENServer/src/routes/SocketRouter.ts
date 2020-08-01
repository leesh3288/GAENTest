module.exports = function(socket, socketDict, io) {
    const socketId = socket.id;

    console.log('Socket connected.');
    socket.emit('clienttype');
    console.log('Emitted client-type');
    console.log(socketDict);

    socket.on('test', function(data) {
        console.log('test function called.');
    })

    // Check client type (device/console)
    socket.on('type-console', function(data) {
        var idx = socketDict['console'];
        console.log("idx: "+idx);
        if (idx == undefined) {
            socketDict['console'] = socketId;
            socket.emit('init-console', {
                data: "init-console-data"
            })
        } else {
            socket.emit('refuse-console');
        }
        console.log(socketDict);
    })

    socket.on('type-device', function(data) {
        // TODO: add information to lists
    })

    socket.on('disconnect', function(data) {
        console.log('Socket disconnected.');
        delete socketDict['console'];
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