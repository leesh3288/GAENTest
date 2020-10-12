import * as express from "express";
import * as asyncHandler from "express-async-handler"
import { Connection, InsertResult } from "typeorm";
import { Log } from "../entity/Log";
import { LogGeneral } from "../entity/LogGeneral";
import { Config } from "../entity/Config"
import { ScanInstanceLog } from "../entity/ScanInstanceLog";

export const RootRouter = express.Router();

RootRouter.get('/config', asyncHandler(async (req, res, next) => {
    let db: Connection = req.app.get('db');
    let config: Config;
    try {
        config = await db.manager.getRepository(Config).findOne();
    } catch (e) {
        res.status(500).send("Failed to fetch config.");
        console.log("Failed to fetch config. Exception:");
        console.log(e);
        return;
    }
    res.status(200).send(config);
}));

RootRouter.put('/log', asyncHandler(async (req, res, next) => {
    let db: Connection = req.app.get('db');
    if (!(req.body instanceof Array)) {
        res.status(400).send("JSON not an array.");
        return;
    }

    const logs: Log[] = [];
    for (const entry of req.body) {
        if (entry === null || entry === undefined || !Number.isInteger(entry.time)) {
            continue;
        }
        const log = new Log();
        log.testId = entry.testId;
        log.myId = entry.myId;
        log.time = new Date(entry.time);
        log.logType = entry.logType;
        log.otherId = entry.otherId;
        log.rssi = entry.rssi;
        log.rssiCorrection = entry.rssiCorrection;
        log.tx = entry.tx;
        log.attenuation = entry.attenuation;
        logs.push(log);
    }
    if (logs.length == 0) {
        res.status(400).send("No well-formed logs to save.");
        return;
    }

    let savedLogs: InsertResult;
    try {
        savedLogs = await db.manager.getRepository(Log)
            .createQueryBuilder()
            .insert()
            .values(logs)
            .useTransaction(true)
            .execute();
    } catch (e) {
        res.status(400).send("Failed to save logs, check if data is well-formed.");
        console.log("Failed to save logs. Exception:");
        console.log(e);
        return;
    }
    res.status(201).send("Saved " + savedLogs.generatedMaps.length + " logs.");
}));

RootRouter.put('/log_gen', asyncHandler(async (req, res, next) => {
    let db: Connection = req.app.get('db');
    if (!(req.body instanceof Array)) {
        res.status(400).send("JSON not an array.");
        return;
    }

    const logs_gen: LogGeneral[] = [];
    for (const entry of req.body) {
        if (entry === null || entry === undefined || !Number.isInteger(entry.time)) {
            continue;
        }
        const log = new LogGeneral();
        log.testId = entry.testId;
        log.myId = entry.myId;
        log.time = new Date(entry.time);
        log.msg = entry.msg;
        logs_gen.push(log);
    }
    if (logs_gen.length == 0) {
        res.status(400).send("No well-formed general logs to save.");
        return;
    }

    let savedLogs: InsertResult;
    try {
        savedLogs = await db.manager.getRepository(LogGeneral)
            .createQueryBuilder()
            .insert()
            .values(logs_gen)
            .useTransaction(true)
            .execute();
    } catch (e) {
        res.status(400).send("Failed to save logs_gen, check if data is well-formed.");
        console.log("Failed to save logs_gen. Exception:");
        console.log(e);
        return;
    }
    res.status(201).send("Saved " + savedLogs.generatedMaps.length + " general logs.");
}));

RootRouter.put('/log_si', asyncHandler(async (req, res, next) => {
    let db: Connection = req.app.get('db');
    if (!(req.body instanceof Array)) {
        res.status(400).send("JSON not an array.");
        return;
    }

    const logs: ScanInstanceLog[] = [];
    for (const entry of req.body) {
        if (entry === null || entry === undefined || !Number.isInteger(entry.time)) {
            continue;
        }
        const log = new ScanInstanceLog();
        log.testId = entry.testId;
        log.myId = entry.myId;
        log.otherId = entry.otherId;
        log.time = new Date(entry.time);
        log.secondsSinceLastScan = entry.secondsSinceLastScan;
        log.typicalAttenuation = entry.typicalAttenuation;
        log.typicalPowerAttenuation = entry.typicalPowerAttenuation;
        log.minAttenuation = entry.minAttenuation;
        logs.push(log);
    }
    if (logs.length == 0) {
        res.status(400).send("No well-formed logs to save.");
        return;
    }

    let savedLogs: InsertResult;
    try {
        savedLogs = await db.manager.getRepository(ScanInstanceLog)
            .createQueryBuilder()
            .insert()
            .values(logs)
            .useTransaction(true)
            .execute();
    } catch (e) {
        res.status(400).send("Failed to save logs, check if data is well-formed.");
        console.log("Failed to save logs. Exception:");
        console.log(e);
        return;
    }
    res.status(201).send("Saved " + savedLogs.generatedMaps.length + " logs.");
}));

RootRouter.get('/db_log', asyncHandler(async (req, res, next) => {
    let db: Connection = req.app.get('db');

    let logs: Log[];
    try {
        logs = await db.manager.getRepository(Log).find();
    } catch (e) {
        res.status(400).send("Failed to fetch log database.");
        console.log("Failed to fetch log database. Exception:");
        console.log(e);
        return;
    }
    res.status(200).send(logs);
}));

RootRouter.get('/db_log_gen', asyncHandler(async (req, res, next) => {
    let db: Connection = req.app.get('db');

    let logs_gen: LogGeneral[];
    try {
        logs_gen = await db.manager.getRepository(LogGeneral).find();
    } catch (e) {
        res.status(400).send("Failed to fetch general log database.");
        console.log("Failed to fetch general log database. Exception:");
        console.log(e);
        return;
    }
    res.status(200).send(logs_gen);
}));

RootRouter.get('/db_log_si', asyncHandler(async (req, res, next) => {
    let db: Connection = req.app.get('db');

    let logs: ScanInstanceLog[];
    try {
        logs = await db.manager.getRepository(ScanInstanceLog).find();
    } catch (e) {
        res.status(400).send("Failed to fetch log database.");
        console.log("Failed to fetch log database. Exception:");
        console.log(e);
        return;
    }
    res.status(200).send(logs);
}));