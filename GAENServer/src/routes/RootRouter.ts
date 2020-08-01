import * as express from "express";
import * as asyncHandler from "express-async-handler"
import { Connection } from "typeorm";
import { Log } from "../entity/Log";
import { Config } from "../entity/Config"

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

    let savedLogs: Log[];
    try {
        savedLogs = await db.manager.getRepository(Log).save(logs);
    } catch (e) {
        res.status(400).send("Failed to save logs, check if data is well-formed.");
        console.log("Failed to save logs. Exception:");
        console.log(e);
        return;
    }
    res.status(201).send("Saved " + savedLogs.length + " logs.");
}));

RootRouter.get('/logdb', asyncHandler(async (req, res, next) => {
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