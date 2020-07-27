import { Entity, PrimaryGeneratedColumn, Column, Index } from "typeorm";
import { IsUUID, IsDate, IsInt, IsOptional } from "class-validator";
import { ValidationEntity } from "./ValidationEntity";

@Entity({name: "logs"})
@Index(["myId", "time", "logType"], { unique: true })
export class Log extends ValidationEntity {
    @PrimaryGeneratedColumn()
    id: number;

    @IsUUID()
    @Column("char", {nullable: false, length: 36})
    myId: string;

    @IsDate()
    @Column("timestamp", {nullable: false})
    time: Date;

    @IsOptional()
    @IsInt()
    @Column("int")
    logType?: number;

    @IsOptional()
    @IsUUID()
    @Column("char", {length: 36})
    otherId?: string;

    @IsOptional()
    @IsInt()
    @Column("int")
    rssi?: number;

    @IsOptional()
    @IsInt()
    @Column("int")
    tx?: number;

    @IsOptional()
    @IsInt()
    @Column("int")
    attenuation?: number;
}
