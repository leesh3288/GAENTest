import {Entity, PrimaryGeneratedColumn, Column, BaseEntity, Index} from "typeorm";

@Entity({name: "logs"})
@Index(["myId", "time", "logType"], { unique: true })
export class Log extends BaseEntity {
    @PrimaryGeneratedColumn()
    id: number;

    @Column("char", {nullable: false, length: 36})
    myId: string;

    @Column("timestamp", {nullable: false})
    time: Date;

    @Column("int")
    logType?: number;

    @Column("char", {length: 36})
    otherId?: number;

    @Column("int")
    rssi?: number;

    @Column("int")
    tx?: number;

    @Column("int")
    attenuation?: number;
}
