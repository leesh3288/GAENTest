import { Entity, PrimaryGeneratedColumn, Column } from "typeorm";
import { IsDate, IsInt, IsOptional, IsString } from "class-validator";
import { ValidationEntity } from "./ValidationEntity";

@Entity({name: "scan_instances"})
export class ScanInstanceLog extends ValidationEntity {
    @PrimaryGeneratedColumn()
    id: number;

    @IsOptional()
    @IsString()
    @Column("char", {length: 100, default: null})
    testId?: string;

    @Column("char", {nullable: false, length: 36})
    @IsString()
    myId: string;

    @IsOptional()
    @IsString()
    @Column("char", {length: 36, default: null})
    otherId?: string;

    @IsDate()
    @Column("timestamp", {nullable: false, precision: 6})
    time: Date;

    @IsOptional()
    @IsInt()
    @Column("int", {nullable: false})
    secondsSinceLastScan: number;

    @IsOptional()
    @IsInt()
    @Column("int", {nullable: false})
    typicalAttenuation: number;

    @IsOptional()
    @IsInt()
    @Column("int", {nullable: false})
    typicalPowerAttenuation: number;

    @IsOptional()
    @IsInt()
    @Column("int", {nullable: false})
    minAttenuation: number;
}
