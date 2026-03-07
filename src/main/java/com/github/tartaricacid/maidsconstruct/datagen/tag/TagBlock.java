package com.github.tartaricacid.maidsconstruct.datagen.tag;


import com.github.tartaricacid.maidsconstruct.MaidsConstruct;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class TagBlock extends BlockTagsProvider {
    public TagBlock(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, MaidsConstruct.MOD_ID, existingFileHelper);
    }

    @Override
    @SuppressWarnings("all")
    protected void addTags(HolderLookup.Provider provider) {
    }
}
