-- Create database
CREATE DATABASE gaen_db;

-- Create new user
CREATE USER 'GAEN'@'localhost' IDENTIFIED BY 'GAENtest';
GRANT ALL PRIVILEGES ON gaen_db.* TO 'GAEN'@'localhost';

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
CREATE TABLE config (
    version INT NOT NULL PRIMARY KEY,
    SCAN_PERIOD BIGINT NOT NULL,
    SCAN_DURATION BIGINT NOT NULL,
    MAX_JITTER BIGINT NOT NULL,
    UPLOAD_PERIOD BIGINT NOT NULL,
    SERVICE_UUID INT NOT NULL,
    advertiseMode INT NOT NULL,
    advertiseTxPower INT NOT NULL,
    scanMode INT NOT NULL
);
CREATE TABLE scan_instances (
    id INT PRIMARY KEY,
    testId CHAR(100),
    myId CHAR(36) NOT NULL,
    otherId CHAR(36),
    time TIMESTAMP(6) NOT NULL,
    secondsSinceLastScan INT NOT NULL,
    typicalAttenuation INT NOT NULL,
    typicalPowerAttenuation INT NOT NULL,
    minAttenuation INT NOT NULL
);

-- Initialize config
INSERT IGNORE INTO config (version, SCAN_PERIOD, SCAN_DURATION, MAX_JITTER, UPLOAD_PERIOD, SERVICE_UUID, advertiseMode, advertiseTxPower, scanMode) 
VALUES (0b01000000, 300000, 8000, 90000, 3600000, 0xfd6f, 1, 1, 2);