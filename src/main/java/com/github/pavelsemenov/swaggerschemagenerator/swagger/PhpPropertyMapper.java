package com.github.pavelsemenov.swaggerschemagenerator.swagger;

import com.github.pavelsemenov.swaggerschemagenerator.psi.PhpClassExtractor;
import com.github.pavelsemenov.swaggerschemagenerator.psi.PhpFieldFilter;
import com.github.pavelsemenov.swaggerschemagenerator.psi.PhpPropertyDescriptionExtractor;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import io.swagger.v3.oas.models.media.*;

import javax.inject.Inject;
import java.util.Optional;

@SuppressWarnings("rawtypes")
public class PhpPropertyMapper {
    private final PhpClassExtractor classExtractor;
    private final PhpFieldFilter fieldFilter;
    private final PhpPropertyDescriptionExtractor descriptionExtractor;

    @Inject
    public PhpPropertyMapper(PhpClassExtractor classExtractor, PhpFieldFilter fieldFilter, PhpPropertyDescriptionExtractor descriptionExtractor) {
        this.classExtractor = classExtractor;
        this.fieldFilter = fieldFilter;
        this.descriptionExtractor = descriptionExtractor;
    }

    public Optional<Schema> createSchema(Field field) {
        return parseType(field, field.getType());
    }

    private Optional<Schema> parseType(Field field, PhpType type) {
        String firstType = fieldFilter.getFirstType(type);
        Optional<Schema> schema = Optional.empty();

        switch (firstType) {
            case PhpType._INTEGER:
            case PhpType._INT:
                schema = Optional.of(new IntegerSchema());
                break;
            case PhpType._FLOAT:
                schema = Optional.of(new NumberSchema().format("float"));
                break;
            case PhpType._NUMBER:
                schema = Optional.of(new NumberSchema());
                break;
            case PhpType._DOUBLE:
                schema = Optional.of(new NumberSchema().format("double"));
                break;
            case PhpType._STRING:
                schema = Optional.of(new StringSchema());
                break;
            case PhpType._BOOLEAN:
            case PhpType._BOOL:
                schema = Optional.of(new BooleanSchema());
                break;
            case PhpType._NULL:
                break;
            default:
                schema = PhpType.isPluralType(firstType) ? parseArray(field, type) : parseClass(field, firstType);
        }
        schema.ifPresent(s -> {
            s.name(field.getName());
            if (type.isNullable()) {
                s.nullable(true);
            }
            descriptionExtractor.extractDescription(field).ifPresent(s::description);
        });

        return schema;
    }

    private Optional<Schema> parseClass(Field field, String type) {
        Optional<PhpClass> refClass = classExtractor.extractFromIndex(type);

        return refClass.map(phpClass -> new ObjectSchema().name(field.getName()).$ref(phpClass.getName())
                .pattern(phpClass.getFQN()));
    }

    private Optional<Schema> parseArray(Field field, PhpType type) {
        PhpType single = type.unpluralize();
        Optional<Schema> itemSchema = parseType(field, single);

        return itemSchema.map(s -> new ArraySchema().items(s));
    }
}
