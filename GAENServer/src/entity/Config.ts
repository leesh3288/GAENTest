import {Entity, Column, BaseEntity, UpdateDateColumn, PrimaryColumn} from "typeorm";

@Entity()
export class Config extends BaseEntity {
    @PrimaryColumn("int", {nullable: false})
    version: number;

    @Column("bigint", {nullable: false})
    SCAN_PERIOD: number;

    @Column("bigint", {nullable: false})
    SCAN_DURATION: number;

    @Column("varchar", {nullable: false, length: 31})
    SERVICE_UUID: string;

    @Column("int", {nullable: false})
    advertiseMode: number;

    @Column("int", {nullable: false})
    advertiseTxPower: number;

    @Column("int", {nullable: false})
    scanMode: number;
}

/*
CREATE TABLE config (
    version INT NOT NULL PRIMARY KEY,
    SCAN_PERIOD BIGINT NOT NULL,
    SCAN_DURATION BIGINT NOT NULL,
    SERVICE_UUID VARCHAR(31) NOT NULL,
    advertiseMode INT NOT NULL,
    advertiseTxPower INT NOT NULL,
    scanMode INT NOT NULL
);
*/