import { Entity, PrimaryGeneratedColumn, Column, Index } from "typeorm";
import { IsDate, IsInt, IsOptional, IsString } from "class-validator";
import { ValidationEntity } from "./ValidationEntity";

@Entity({name: "logs"})
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
    @Column("int", {default: null})
    logType?: number;

    @IsOptional()
    @IsString()
    @Column("char", {length: 36, default: null})
    otherId?: string;

    @IsOptional()
    @IsString()
    @Column("char", {length: 100, default: null})
    testId?: string;

    @IsOptional()
    @IsInt()
    @Column("int", {default: null})
    rssi?: number;

    @IsOptional()
    @IsInt()
    @Column("int", {default: null})
    rssiCorrection?: number;

    @IsOptional()
    @IsInt()
    @Column("int", {default: null})
    tx?: number;

    @IsOptional()
    @IsInt()
    @Column("int", {default: null})
    attenuation?: number;
}
