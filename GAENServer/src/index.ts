import "reflect-metadata";
import * as express from "express";
import * as ejs from "ejs"
import * as bodyParser from "body-parser"
import * as cors from "cors"
import {createConnection} from "typeorm";
import {RootRouter} from "./routes/RootRouter"
import {Config} from "./entity/Config"


const app = express();

app.set('views', __dirname + '/views');
app.set('view engine', 'ejs');
app.engine('html', ejs.renderFile);
app.use(express.static('public'));

app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: false }));
app.use(cors());


console.log("Connecting to MySQL DB...");
createConnection().then(async db => {
    console.log("Connected to MySQL DB.");

    console.log("Initializing config...");
    
    const defaultConfig = new Config();
    defaultConfig.version = 0b01000000;
    defaultConfig.SCAN_PERIOD = (5 * 60 * 1000).toString();
    defaultConfig.SCAN_DURATION = (8 * 1000).toString();
    defaultConfig.SERVICE_UUID = 0xfd6f;
    defaultConfig.advertiseMode = 1;
    defaultConfig.advertiseTxPower = 1;
    defaultConfig.scanMode = 1;

    await db.manager.getRepository(Config)
        .createQueryBuilder()
        .insert()
        .orIgnore()
        .into(Config)
        .values(defaultConfig)
        .execute();
    
    console.log("Initialized config.");

    app.listen(80, () => console.log("Express server has started on port 80"))

    app.set('db', db);

    app.use('/', RootRouter);

}).catch(error => console.log(error));
