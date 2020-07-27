import {Entity, PrimaryGeneratedColumn, Column, BaseEntity, Index, UpdateDateColumn} from "typeorm";

@Entity({name: "logs"})
@Index(["myId", "time", "logType"], { unique: true })
export class Log extends BaseEntity {
    @PrimaryGeneratedColumn()
    id: number;

    @Column("varchar", {nullable: false, length: 31})
    myId: string;

    @UpdateDateColumn({
        nullable: false,
        type: "timestamp",
        default: () => "CURRENT_TIMESTAMP(6)",
        onUpdate: "CURRENT_TIMESTAMP(6)"
    })
    time: Date;

    @Column("int")
    logType?: number;

    @Column("varchar", {length: 31})
    otherId?: number;

    @Column("int")
    rssi?: number;

    @Column("int")
    tx?: number;
}
