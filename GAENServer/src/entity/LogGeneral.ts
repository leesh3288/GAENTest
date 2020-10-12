import { Entity, PrimaryGeneratedColumn, Column } from "typeorm";
import { IsDate, IsInt, IsOptional, IsString } from "class-validator";
import { ValidationEntity } from "./ValidationEntity";

@Entity({name: "logs_general"})
export class LogGeneral extends ValidationEntity {
    @PrimaryGeneratedColumn()
    id: number;

    @Column("char", {nullable: false, length: 36})
    @IsString()
    myId: string;

    @IsDate()
    @Column("timestamp", {nullable: false, precision: 6})
    time: Date;

    @IsOptional()
    @IsString()
    @Column("char", {length: 100, default: null})
    testId?: string;

    @IsOptional()
    @IsString()
    @Column("char", {length: 100, default: null})
    msg?: string;
}
