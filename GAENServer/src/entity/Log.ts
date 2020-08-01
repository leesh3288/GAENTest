import { Entity, PrimaryGeneratedColumn, Column, Index } from "typeorm";
import { IsUUID, IsDate, IsInt, IsOptional, IsString } from "class-validator";
import { ValidationEntity } from "./ValidationEntity";

@Entity({name: "logs"})
@Index(["myId", "time", "logType"], { unique: true })
export class Log extends ValidationEntity {
    @PrimaryGeneratedColumn()
    id: number;

    @Column("char", {nullable: false, length: 36})
    @IsString()
    myId: string;

    @IsDate()
    @Column("timestamp", {nullable: false, precision: 6})
    time: Date;

    @IsOptional()
    @IsInt()
    @Column("int")
    logType?: number;

    @IsOptional()
    @IsString()
    @Column("char", {length: 36})
    otherId?: string;

    @IsOptional()
    @IsString()
    @Column("char", {length: 100})
    testId?: string;

    @IsOptional()
    @IsInt()
    @Column("int")
    rssi?: number;

    @IsOptional()
    @IsInt()
    @Column("int")
    rssiCorrection?: number;

    @IsOptional()
    @IsInt()
    @Column("int")
    tx?: number;

    @IsOptional()
    @IsInt()
    @Column("int")
    attenuation?: number;
}
