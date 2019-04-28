package nl.rutgerkok.worldgeneratorapi.internal.bukkitoverrides;

import java.util.List;
import java.util.Objects;

import org.bukkit.generator.ChunkGenerator.BiomeGrid;
import org.bukkit.generator.ChunkGenerator.ChunkData;

import net.minecraft.server.v1_14_R1.BiomeBase;
import net.minecraft.server.v1_14_R1.BiomeBase.BiomeMeta;
import net.minecraft.server.v1_14_R1.BlockPosition;
import net.minecraft.server.v1_14_R1.ChunkCoordIntPair;
import net.minecraft.server.v1_14_R1.ChunkGeneratorAbstract;
import net.minecraft.server.v1_14_R1.EnumCreatureType;
import net.minecraft.server.v1_14_R1.GeneratorAccess;
import net.minecraft.server.v1_14_R1.GeneratorSettingsDefault;
import net.minecraft.server.v1_14_R1.HeightMap.Type;
import net.minecraft.server.v1_14_R1.IChunkAccess;
import net.minecraft.server.v1_14_R1.MobSpawnerCat;
import net.minecraft.server.v1_14_R1.MobSpawnerPatrol;
import net.minecraft.server.v1_14_R1.MobSpawnerPhantom;
import net.minecraft.server.v1_14_R1.NoiseGenerator3;
import net.minecraft.server.v1_14_R1.ProtoChunk;
import net.minecraft.server.v1_14_R1.RegionLimitedWorldAccess;
import net.minecraft.server.v1_14_R1.SeededRandom;
import net.minecraft.server.v1_14_R1.SpawnerCreature;
import net.minecraft.server.v1_14_R1.WorldGenStage;
import net.minecraft.server.v1_14_R1.WorldGenerator;
import net.minecraft.server.v1_14_R1.WorldServer;
import nl.rutgerkok.worldgeneratorapi.BaseChunkGenerator;
import nl.rutgerkok.worldgeneratorapi.BaseChunkGenerator.GeneratingChunk;
import nl.rutgerkok.worldgeneratorapi.BiomeGenerator;
import nl.rutgerkok.worldgeneratorapi.decoration.BaseDecorationType;
import nl.rutgerkok.worldgeneratorapi.internal.BiomeGeneratorImpl;
import nl.rutgerkok.worldgeneratorapi.internal.WorldDecoratorImpl;

public final class InjectedChunkGenerator extends ChunkGeneratorAbstract<GeneratorSettingsDefault> {

    private static class GeneratingChunkImpl implements GeneratingChunk {

        private final int chunkX;
        private final int chunkZ;
        private final ChunkDataImpl blocks;
        private final BiomeGenerator biomeGenerator;
        private final BiomeGridImpl biomeGrid;

        GeneratingChunkImpl(ProtoChunk internal, BiomeGenerator biomeGenerator) {
            this.chunkX = internal.getPos().x;
            this.chunkZ = internal.getPos().z;
            this.blocks = new ChunkDataImpl(internal);
            this.biomeGrid = new BiomeGridImpl(internal.getBiomeIndex());

            this.biomeGenerator = biomeGenerator;
        }

        @Override
        public BiomeGenerator getBiomeGenerator() {
            return biomeGenerator;
        }

        @Override
        public BiomeGrid getBiomesForChunk() {
            return biomeGrid;
        }

        @Override
        public ChunkData getBlocksForChunk() {
            return blocks;
        }

        @Override
        public int getChunkX() {
            return chunkX;
        }

        @Override
        public int getChunkZ() {
            return chunkZ;
        }

    }

    private final org.bukkit.World world;
    /**
     * Could someone ask Mojang why world generation controls these mobs?
     */
    private final MobSpawnerPhantom phantomSpawner = new MobSpawnerPhantom();
    private final MobSpawnerPatrol patrolSpawner = new MobSpawnerPatrol();
    private final MobSpawnerCat catSpawner = new MobSpawnerCat();

    private final NoiseGenerator3 surfaceNoise;
    public final WorldDecoratorImpl worldDecorator = new WorldDecoratorImpl();
    private BaseChunkGenerator baseChunkGenerator;

    private final BiomeGenerator biomeGenerator;

    public InjectedChunkGenerator(WorldServer world, BaseChunkGenerator baseChunkGenerator) {
        super(world, world.getChunkProvider().getChunkGenerator().getWorldChunkManager(),
                4, 8, 256, world.getChunkProvider().getChunkGenerator().getSettings(), true);
        this.world = world.getWorld();

        SeededRandom seededrandom = new SeededRandom(this.seed);
        surfaceNoise = new NoiseGenerator3(seededrandom, 4);

        this.biomeGenerator = new BiomeGeneratorImpl(world.getChunkProvider()
                .getChunkGenerator().getWorldChunkManager());
        setBaseChunkGenerator(baseChunkGenerator);
    }

    @Override
    protected double a(double d0, double d1, int i) {
        // No idea what this is calculating - we only know that it has got something to
        // do with terrain shape
        double d3 = (i - (8.5D + d0 * 8.5D / 8.0D * 4.0D)) * 12.0D * 128.0D / 256.0D / d1;
        if (d3 < 0.0D) {
            d3 *= 4.0D;
        }

        return d3;
    }


    @Override
    protected void a(double[] adouble, int i, int j) {
        // No idea what this is calculating - but it has got something to do with
        // terrain shape
        this.a(adouble, i, j, 684.4119873046875D, 684.4119873046875D, 8.555149841308594D, 4.277574920654297D, 3, -10);
    }

    @Override
    protected double[] a(int i, int j) {
        // No idea what this is calculating - but it has got something to do with
        // terrain shape
        throw new UnsupportedOperationException("Not supported, sorry!");
    }

    @Override
    public void addDecorations(RegionLimitedWorldAccess populationArea) {
        this.worldDecorator.spawnDecorations(this, populationArea);
    }

    @Override
    public void addMobs(RegionLimitedWorldAccess regionlimitedworldaccess) {
        final int i = regionlimitedworldaccess.a();
        final int j = regionlimitedworldaccess.b();
        final BiomeBase biomebase = regionlimitedworldaccess.getChunkAt(i, j).getBiomeIndex()[0];
        final SeededRandom seededrandom = new SeededRandom();
        seededrandom.a(regionlimitedworldaccess.getSeed(), i << 4, j << 4);
        SpawnerCreature.a(regionlimitedworldaccess, biomebase, i, j, seededrandom);
    }

    @Override
    public void buildBase(IChunkAccess ichunkaccess) {
        ChunkCoordIntPair chunkcoordintpair = ichunkaccess.getPos();
        int k = chunkcoordintpair.x;
        int l = chunkcoordintpair.z;
        SeededRandom seededrandom = new SeededRandom();
        seededrandom.a(k, l);
        
        // Generate early decorations
        GeneratingChunkImpl chunk = new GeneratingChunkImpl((ProtoChunk) ichunkaccess, biomeGenerator);
        this.worldDecorator.spawnCustomBaseDecorations(BaseDecorationType.RAW_GENERATION, chunk);

        // Generate surface
        if (this.worldDecorator.isDefaultEnabled(BaseDecorationType.SURFACE)) {
            BiomeBase[] abiomebase = ichunkaccess.getBiomeIndex();
            for (int i1 = 0; i1 < 16; ++i1) {
                for (int j1 = 0; j1 < 16; ++j1) {
                    int k1 = k + i1;
                    int l1 = l + j1;
                    int i2 = ichunkaccess.a(Type.WORLD_SURFACE_WG, i1, j1) + 1;
                    double d1 = this.surfaceNoise.a(k1 * 0.0625D, l1 * 0.0625D, 0.0625D, i1 * 0.0625D);
                    abiomebase[j1 * 16 + i1].a(seededrandom, ichunkaccess, k1, l1, i2, d1, this.getSettings().r(),
                            this.getSettings().s(), this.getSeaLevel(), this.a.getSeed());
                }
            }
        }
        this.worldDecorator.spawnCustomBaseDecorations(BaseDecorationType.SURFACE, chunk);

        // Generate bedrock
        if (this.worldDecorator.isDefaultEnabled(BaseDecorationType.BEDROCK)) {
            this.a(ichunkaccess, seededrandom);
        }
        this.worldDecorator.spawnCustomBaseDecorations(BaseDecorationType.BEDROCK, chunk);
    }

    @Override
    public void buildNoise(GeneratorAccess generatoraccess, IChunkAccess ichunkaccess) {
        // Generate blocks
        GeneratingChunkImpl chunk = new GeneratingChunkImpl((ProtoChunk) ichunkaccess, biomeGenerator);
        baseChunkGenerator.setBlocksInChunk(chunk);
    }

    @Override
    public void doCarving(IChunkAccess world, WorldGenStage.Features stage) {
        this.worldDecorator.spawnCarvers(world, stage, this.getSeaLevel(), this.seed);
    }

    @Override
    public void doMobSpawning(WorldServer worldserver, boolean flag, boolean flag1) {
        this.phantomSpawner.a(worldserver, flag, flag1);
        this.patrolSpawner.a(worldserver, flag, flag1);
        this.catSpawner.a(worldserver, flag, flag1);
    }

    public BaseChunkGenerator getBaseChunkGenerator() {
        return baseChunkGenerator;
    }

    public BiomeGenerator getBiomeGenerator() {
        return biomeGenerator;
    }

    @Override
    public List<BiomeMeta> getMobsFor(EnumCreatureType enumcreaturetype, BlockPosition blockposition) {
        if (WorldGenerator.SWAMP_HUT.c(this.a, blockposition)) {
            if (enumcreaturetype == EnumCreatureType.MONSTER) {
                return WorldGenerator.SWAMP_HUT.e();
            }

            if (enumcreaturetype == EnumCreatureType.CREATURE) {
                return WorldGenerator.SWAMP_HUT.f();
            }
        } else if (enumcreaturetype == EnumCreatureType.MONSTER) {
            if (WorldGenerator.PILLAGER_OUTPOST.a(this.a, blockposition)) {
                return WorldGenerator.PILLAGER_OUTPOST.e();
            }

            if (WorldGenerator.OCEAN_MONUMENT.a(this.a, blockposition)) {
                return WorldGenerator.OCEAN_MONUMENT.e();
            }
        }

        return super.getMobsFor(enumcreaturetype, blockposition);
    }

    @Override
    public int getSeaLevel() {
        return world.getSeaLevel();
    }

    @Override
    public int getSpawnHeight() {
        return world.getSeaLevel() + 1;
    }

    public void setBaseChunkGenerator(BaseChunkGenerator baseChunkGenerator) {
        this.baseChunkGenerator = Objects.requireNonNull(baseChunkGenerator, "baseChunkGenerator");
    }

}