import {Entity, Column, BaseEntity, PrimaryColumn} from "typeorm";

@Entity()
export class Config extends BaseEntity {
    @PrimaryColumn("int", {nullable: false})
    version: number;

    @Column("bigint", {nullable: false})
    SCAN_PERIOD: string;

    @Column("bigint", {nullable: false})
    SCAN_DURATION: string;

    @Column("bigint", {nullable: false})
    MAX_JITTER: string;

    // Server upload frequency in ms. Set to 0 for immediate upload after scan completion.
    @Column("bigint", {nullable: false})
    UPLOAD_PERIOD: string;

    @Column("int", {nullable: false})
    SERVICE_UUID: number;

    @Column("int", {nullable: false})
    advertiseMode: number;

    @Column("int", {nullable: false})
    advertiseTxPower: number;

    @Column("int", {nullable: false})
    scanMode: number;
}
