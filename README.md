# BBLib

BBLib is a Minecraft library, inspired by Geckolib, for rendering and animating Blockbench `.bbmodel's Generic Model` files directly in-game.
  
This has 2 main purposes:
- Add support for a large amount of currently unsupported models (such as MCModels.net)
- Support latest Blockbench V5.x features everywhere


## Add BBLib to your project (Forge Example)

### Gradle

```gradle
repositories {
    maven { url "https://cursemaven.com" }
}

dependencies {
    implementation fg.deobf("curse.maven:blockbenchlib-1483753:7744542")
}
```

### Maven

```xml
<repositories>
    <repository>
        <id>cursemaven</id>
        <url>https://cursemaven.com</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>curse.maven</groupId>
        <artifactId>blockbenchlib-1483753</artifactId>
        <version>7744542</version>
    </dependency>
</dependencies>
```

## Asset Convention

BBLib scans client resources for Blockbench files in:

```text
assets/<modid>/bbmodels/*.bbmodel
```

Example:

```text
assets/examplemod/bbmodels/my_entity.bbmodel
```

That file is exposed to the API as:

```java
ResourceLocation.fromNamespaceAndPath("examplemod", "my_entity")
```

Embedded Blockbench textures are supported. If you want to use your own texture instead, override `getTextureResource(...)` in your model class.

## Quick Start

### 1. Make the entity animatable

```java
public class MyEntity extends PathfinderMob implements BBEntityAnimatable {
    private final AnimatableInstanceCache cache = BBLibApi.createCache(this);

    protected MyEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @Override
    public void registerControllers(ControllerRegistrar controllers) {
        controllers.add(DefaultAnimations.genericWalkIdleController(
                this, "main", 5, DefaultAnimations.WALK, DefaultAnimations.IDLE
        ));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
```

### 2. Point a `BBModel` at the `.bbmodel` asset

```java
public class MyEntityModel extends BBModel<MyEntity> {
    @Override
    public ResourceLocation getModelResource(MyEntity entity) {
        return ResourceLocation.fromNamespaceAndPath("examplemod", "my_entity");
    }
}
```

### 3. Render it with `BBEntityRenderer`

```java
public class MyEntityRenderer extends BBEntityRenderer<MyEntity> {
    public MyEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new MyEntityModel());
    }
}
```

From there you can:

- use `AnimationController` for custom state logic
- call `triggerAnimation(...)` on `BBEntityAnimatable` entities
- override `applyMolangQueries(...)` to inject custom query values
- override `setCustomAnimations(...)` to manipulate specific bones
- add custom `BBRenderLayer`s for extra visual passes
- and more ..

## License

This project is licensed under the MIT License. See `LICENSE.txt`.
