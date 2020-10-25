-- Create database
CREATE DATABASE gaen_db;

-- Create new user
CREATE USER 'GAEN'@'172.0.0.0/255.0.0.0' IDENTIFIED BY 'GAENtest';
GRANT ALL PRIVILEGES ON gaen_db.* TO 'GAEN'@'172.0.0.0/255.0.0.0';

-- Log in as new user

-- Create tables
use gaen_db;
CREATE TABLE logs (
    id INT PRIMARY KEY,
    testId CHAR(100),
    myId CHAR(36) NOT NULL,
    otherId CHAR(36),
    time TIMESTAMP(6) NOT NULL,
    logType INT,    
    rssi INT,
    tx INT,
    rssiCorrection INT,
    attenuation INT,
    UNIQUE KEY (myId, otherId, time, logType)
);
CREATE TABLE logs_general (
    id INT PRIMARY KEY,
    testId CHAR(100),
    myId CHAR(36) NOT NULL,
    time TIMESTAMP(6) NOT NULL,
    msg CHAR(100)
);
CREATE TABLE config (
    version INT NOT NULL PRIMARY KEY,
    SCAN_PERIOD BIGINT NOT NULL,
    SCAN_DURATION BIGINT NOT NULL,
    MAX_JITTER BIGINT NOT NULL,
    UPLOAD_PERIOD BIGINT NOT NULL,
    SERVICE_UUID INT NOT NULL,
    advertiseMode INT NOT NULL,
    advertiseTxPower INT NOT NULL,
    scanMode INT NOT NULL,
    initJitter BOOL NOT NULL
);
CREATE TABLE scan_instances (
    id INT PRIMARY KEY,
    testId CHAR(100),
    myId CHAR(36) NOT NULL,
    otherId CHAR(36),
    time TIMESTAMP(6) NOT NULL,
    secondsSinceLastScan INT,
    typicalAttenuation INT,
    typicalPowerAttenuation INT,
    minAttenuation INT,
    count INT
);

-- Initialize config
INSERT IGNORE INTO config (version, SCAN_PERIOD, SCAN_DURATION, MAX_JITTER, UPLOAD_PERIOD, SERVICE_UUID, advertiseMode, advertiseTxPower, scanMode, initJitter) VALUES (0b01000000, 300000, 4000, 90000, 3600000, 0xfd6f, 1, 1, 2, TRUE);

