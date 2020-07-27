import * as express from "express";
import * as asyncHandler from "express-async-handler"
import {Connection} from "typeorm";
import {Log} from "../entity/Log";
import {Config} from "../entity/Config"

export const RootRouter = express.Router();

RootRouter.get('/config', asyncHandler(async (req, res, next) => {
    let db: Connection = req.app.get('db');
    let config: Config;
    try {
        config = await db.manager.getRepository(Config).findOne();
    } catch {
        res.status(500).send("Failed to fetch config.");
        console.log("Failed to fetch config, check DB status!");
        return;
    }
    res.status(200).send(config);
}));

RootRouter.put('/log', asyncHandler(async (req, res, next) => {
    let db: Connection = req.app.get('db');
    console.log(req.body);
}));