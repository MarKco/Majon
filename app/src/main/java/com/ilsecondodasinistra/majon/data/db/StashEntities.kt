package com.ilsecondodasinistra.majon.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.ilsecondodasinistra.majon.domain.model.MaterialShare
import com.ilsecondodasinistra.majon.domain.model.Needle
import com.ilsecondodasinistra.majon.domain.model.NeedleMaterial
import com.ilsecondodasinistra.majon.domain.model.NeedleType
import com.ilsecondodasinistra.majon.domain.model.StitchPattern
import com.ilsecondodasinistra.majon.domain.model.Swatch
import com.ilsecondodasinistra.majon.domain.model.Yarn
import com.ilsecondodasinistra.majon.domain.model.YarnMaterial
import com.ilsecondodasinistra.majon.domain.model.YarnWeight

@Entity(tableName = "yarns")
data class YarnEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val brand: String?,
    val colorName: String?,
    val colorArgb: Int?,
    val dyeLot: String?,
    val weight: String?,
    val skeinWeightG: Int?,
    val skeinLengthM: Int?,
    val stockSkeins: Double,
    val pricePerSkein: Double?,
    val shop: String?,
    val care: String?,
    val notes: String?,
    val photoPath: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "yarn_materials",
    foreignKeys = [
        ForeignKey(
            entity = YarnEntity::class,
            parentColumns = ["id"],
            childColumns = ["yarnId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("yarnId")],
)
data class YarnMaterialEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val yarnId: Long,
    val material: String,
    val customName: String?,
    val percent: Int?,
    /** Keeps the order the user entered the blend in. */
    val position: Int,
)

@Entity(
    tableName = "swatches",
    foreignKeys = [
        ForeignKey(
            entity = YarnEntity::class,
            parentColumns = ["id"],
            childColumns = ["yarnId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("yarnId")],
)
data class SwatchEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val yarnId: Long,
    val stitches: Int,
    val rows: Int,
    val needleSizeMm: Double?,
    val pattern: String,
    val weightG: Double?,
    val notes: String?,
)

@Entity(tableName = "needles")
data class NeedleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val type: String,
    val material: String?,
    val sizeMm: Double?,
    val lengthCm: Int?,
    val quantity: Int,
    val brand: String?,
    val notes: String?,
)

data class YarnWithMaterialsEntity(
    @Embedded val yarn: YarnEntity,
    @Relation(parentColumn = "id", entityColumn = "yarnId")
    val materials: List<YarnMaterialEntity>,
)

// --- mappers (unknown enum names fall back, so newer data never crashes an older app) ---

private inline fun <reified E : Enum<E>> enumOrNull(name: String?): E? =
    name?.let { n -> enumValues<E>().firstOrNull { it.name == n } }

fun YarnWithMaterialsEntity.toDomain() = Yarn(
    id = yarn.id,
    name = yarn.name,
    brand = yarn.brand,
    colorName = yarn.colorName,
    colorArgb = yarn.colorArgb,
    dyeLot = yarn.dyeLot,
    weight = enumOrNull<YarnWeight>(yarn.weight),
    materials = materials.sortedBy { it.position }.map {
        MaterialShare(
            material = enumOrNull<YarnMaterial>(it.material) ?: YarnMaterial.OTHER,
            customName = it.customName,
            percent = it.percent,
        )
    },
    skeinWeightG = yarn.skeinWeightG,
    skeinLengthM = yarn.skeinLengthM,
    stockSkeins = yarn.stockSkeins,
    pricePerSkein = yarn.pricePerSkein,
    shop = yarn.shop,
    care = yarn.care,
    notes = yarn.notes,
    photoPath = yarn.photoPath,
    createdAt = yarn.createdAt,
    updatedAt = yarn.updatedAt,
)

fun Yarn.toEntity() = YarnEntity(
    id = id,
    name = name,
    brand = brand,
    colorName = colorName,
    colorArgb = colorArgb,
    dyeLot = dyeLot,
    weight = weight?.name,
    skeinWeightG = skeinWeightG,
    skeinLengthM = skeinLengthM,
    stockSkeins = stockSkeins,
    pricePerSkein = pricePerSkein,
    shop = shop,
    care = care,
    notes = notes,
    photoPath = photoPath,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun Yarn.materialEntities(yarnId: Long) = materials.mapIndexed { index, share ->
    YarnMaterialEntity(
        yarnId = yarnId,
        material = share.material.name,
        customName = share.customName,
        percent = share.percent,
        position = index,
    )
}

fun SwatchEntity.toDomain() = Swatch(
    id = id,
    yarnId = yarnId,
    stitches = stitches,
    rows = rows,
    needleSizeMm = needleSizeMm,
    pattern = enumOrNull<StitchPattern>(pattern) ?: StitchPattern.OTHER,
    weightG = weightG,
    notes = notes,
)

fun Swatch.toEntity() = SwatchEntity(
    id = id,
    yarnId = yarnId,
    stitches = stitches,
    rows = rows,
    needleSizeMm = needleSizeMm,
    pattern = pattern.name,
    weightG = weightG,
    notes = notes,
)

fun NeedleEntity.toDomain() = Needle(
    id = id,
    type = enumOrNull<NeedleType>(type) ?: NeedleType.CIRCULAR,
    material = enumOrNull<NeedleMaterial>(material),
    sizeMm = sizeMm,
    lengthCm = lengthCm,
    quantity = quantity,
    brand = brand,
    notes = notes,
)

fun Needle.toEntity() = NeedleEntity(
    id = id,
    type = type.name,
    material = material?.name,
    sizeMm = sizeMm,
    lengthCm = lengthCm,
    quantity = quantity,
    brand = brand,
    notes = notes,
)
