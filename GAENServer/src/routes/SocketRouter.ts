module.exports = function(socket, socketList, deviceList, io) {
    const socketId = socket.id;

    console.log('Socket connected.');

    // Check client type (device/console)
    socket.on('type-console', function(data) {
        const idx = deviceList.indexOf('console');
        if (idx == -1) {
            socket.emit('init-console', {
                data: "1234"
            })
        } else {
            socket.emit('refuse-console');
        }
    })

    socket.on('disconnect', function(data) {
        console.log('Socket disconnected.');
        removeDev(socketList, deviceList, socketId);
        console.log(socketList);
    })
}

function removeDev(socketList, deviceList, devName) {
    const index = deviceList.indexOf(devName);
    if (index > -1) {
        socketList.splice(index, 1);
        deviceList.splice(index, 1);
    }
}