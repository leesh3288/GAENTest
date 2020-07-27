import {Entity, Column, BaseEntity, UpdateDateColumn, PrimaryColumn} from "typeorm";

@Entity()
export class Config extends BaseEntity {
    @PrimaryColumn("int", {nullable: false})
    version: number;

    @Column("bigint", {nullable: false})
    SCAN_PERIOD: string;

    @Column("bigint", {nullable: false})
    SCAN_DURATION: string;

    @Column("int", {nullable: false})
    SERVICE_UUID: number;

    @Column("int", {nullable: false})
    advertiseMode: number;

    @Column("int", {nullable: false})
    advertiseTxPower: number;

    @Column("int", {nullable: false})
    scanMode: number;
}
