package com.kobylynskyi.graphql.codegen;

import com.kobylynskyi.graphql.codegen.mapper.EnumDefinitionToDataModelMapper;
import com.kobylynskyi.graphql.codegen.mapper.FieldDefinitionToParameterMapper;
import com.kobylynskyi.graphql.codegen.mapper.FieldDefinitionsToResolverDataModelMapper;
import com.kobylynskyi.graphql.codegen.mapper.InputDefinitionToDataModelMapper;
import com.kobylynskyi.graphql.codegen.mapper.InterfaceDefinitionToDataModelMapper;
import com.kobylynskyi.graphql.codegen.mapper.RequestResponseDefinitionToDataModelMapper;
import com.kobylynskyi.graphql.codegen.mapper.TypeDefinitionToDataModelMapper;
import com.kobylynskyi.graphql.codegen.mapper.UnionDefinitionToDataModelMapper;
import com.kobylynskyi.graphql.codegen.model.ApiNamePrefixStrategy;
import com.kobylynskyi.graphql.codegen.model.ApiRootInterfaceStrategy;
import com.kobylynskyi.graphql.codegen.model.GeneratedInformation;
import com.kobylynskyi.graphql.codegen.model.MappingConfig;
import com.kobylynskyi.graphql.codegen.model.MappingConfigConstants;
import com.kobylynskyi.graphql.codegen.model.MappingContext;
import com.kobylynskyi.graphql.codegen.model.definitions.ExtendedDocument;
import com.kobylynskyi.graphql.codegen.model.definitions.ExtendedEnumTypeDefinition;
import com.kobylynskyi.graphql.codegen.model.definitions.ExtendedFieldDefinition;
import com.kobylynskyi.graphql.codegen.model.definitions.ExtendedInputObjectTypeDefinition;
import com.kobylynskyi.graphql.codegen.model.definitions.ExtendedInterfaceTypeDefinition;
import com.kobylynskyi.graphql.codegen.model.definitions.ExtendedObjectTypeDefinition;
import com.kobylynskyi.graphql.codegen.model.definitions.ExtendedScalarTypeDefinition;
import com.kobylynskyi.graphql.codegen.model.definitions.ExtendedUnionTypeDefinition;
import com.kobylynskyi.graphql.codegen.supplier.MappingConfigSupplier;
import com.kobylynskyi.graphql.codegen.utils.Utils;
import graphql.language.FieldDefinition;
import graphql.language.ScalarTypeExtensionDefinition;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

/**
 * Generator of:
 * - Interface for each GraphQL query, mutation, subscription, union and field resolvers
 * - POJO Class for each GraphQL type and input
 * - Enum Class for each GraphQL enum
 *
 * @author kobylynskyi
 * @author valinhadev
 */
public class GraphQLCodegen {

    private final List<String> schemas;
    private final File outputDir;
    private final MappingConfig mappingConfig;
    private final GeneratedInformation generatedInformation;

    public GraphQLCodegen(List<String> schemas,
                          File outputDir,
                          MappingConfig mappingConfig,
                          GeneratedInformation generatedInformation) {
        this(schemas, outputDir, mappingConfig, null, generatedInformation);
    }

    public GraphQLCodegen(List<String> schemas,
                          File outputDir,
                          MappingConfig mappingConfig,
                          MappingConfigSupplier externalMappingConfigSupplier) {
        this(schemas, outputDir, mappingConfig, externalMappingConfigSupplier, new GeneratedInformation());
    }

    public GraphQLCodegen(List<String> schemas,
                          File outputDir,
                          MappingConfig mappingConfig,
                          MappingConfigSupplier externalMappingConfigSupplier,
                          GeneratedInformation generatedInformation) {
        this.schemas = schemas;
        this.outputDir = outputDir;
        this.mappingConfig = mappingConfig;
        this.mappingConfig.combine(externalMappingConfigSupplier != null ? externalMappingConfigSupplier.get() : null);
        initDefaultValues(mappingConfig);
        validateConfigs(mappingConfig);
        sanitizeValues(mappingConfig);
        this.generatedInformation = generatedInformation;
    }

    private static void initDefaultValues(MappingConfig mappingConfig) {
        if (mappingConfig.getModelValidationAnnotation() == null) {
            mappingConfig.setModelValidationAnnotation(MappingConfigConstants.DEFAULT_VALIDATION_ANNOTATION);
        }
        if (mappingConfig.getGenerateBuilder() == null) {
            mappingConfig.setGenerateBuilder(MappingConfigConstants.DEFAULT_BUILDER);
        }
        if (mappingConfig.getGenerateEqualsAndHashCode() == null) {
            mappingConfig.setGenerateEqualsAndHashCode(MappingConfigConstants.DEFAULT_EQUALS_AND_HASHCODE);
        }
        if (mappingConfig.getGenerateClient() == null) {
            mappingConfig.setGenerateClient(MappingConfigConstants.DEFAULT_GENERATE_CLIENT);
        }
        if (mappingConfig.getRequestSuffix() == null) {
            mappingConfig.setRequestSuffix(MappingConfigConstants.DEFAULT_REQUEST_SUFFIX);
        }
        if (mappingConfig.getResponseSuffix() == null) {
            mappingConfig.setResponseSuffix(MappingConfigConstants.DEFAULT_RESPONSE_SUFFIX);
        }
        if (mappingConfig.getResponseProjectionSuffix() == null) {
            mappingConfig.setResponseProjectionSuffix(MappingConfigConstants.DEFAULT_RESPONSE_PROJECTION_SUFFIX);
        }
        if (mappingConfig.getParametrizedInputSuffix() == null) {
            mappingConfig.setParametrizedInputSuffix(MappingConfigConstants.DEFAULT_PARAMETRIZED_INPUT_SUFFIX);
        }
        if (mappingConfig.getGenerateImmutableModels() == null) {
            mappingConfig.setGenerateImmutableModels(MappingConfigConstants.DEFAULT_GENERATE_IMMUTABLE_MODELS);
        }
        if (mappingConfig.getGenerateToString() == null) {
            mappingConfig.setGenerateToString(MappingConfigConstants.DEFAULT_TO_STRING);
        }
        if (mappingConfig.getGenerateApis() == null) {
            mappingConfig.setGenerateApis(MappingConfigConstants.DEFAULT_GENERATE_APIS);
        }
        if (mappingConfig.getApiNameSuffix() == null) {
            mappingConfig.setApiNameSuffix(MappingConfigConstants.DEFAULT_RESOLVER_SUFFIX);
        }
        if (mappingConfig.getTypeResolverSuffix() == null) {
            mappingConfig.setTypeResolverSuffix(MappingConfigConstants.DEFAULT_RESOLVER_SUFFIX);
        }
        if (mappingConfig.getGenerateAsyncApi() == null) {
            mappingConfig.setGenerateAsyncApi(MappingConfigConstants.DEFAULT_GENERATE_ASYNC_APIS);
        }
        if (mappingConfig.getApiAsyncReturnType() == null) {
            mappingConfig.setApiAsyncReturnType(MappingConfigConstants.DEFAULT_API_ASYNC_RETURN_TYPE);
        }
        if (mappingConfig.getGenerateParameterizedFieldsResolvers() == null) {
            mappingConfig.setGenerateParameterizedFieldsResolvers(MappingConfigConstants.DEFAULT_GENERATE_PARAMETERIZED_FIELDS_RESOLVERS);
        }
        if (mappingConfig.getGenerateExtensionFieldsResolvers() == null) {
            mappingConfig.setGenerateExtensionFieldsResolvers(MappingConfigConstants.DEFAULT_GENERATE_EXTENSION_FIELDS_RESOLVERS);
        }
        if (mappingConfig.getGenerateDataFetchingEnvironmentArgumentInApis() == null) {
            mappingConfig.setGenerateDataFetchingEnvironmentArgumentInApis(MappingConfigConstants.DEFAULT_GENERATE_DATA_FETCHING_ENV);
        }
        if (mappingConfig.getGenerateModelsForRootTypes() == null) {
            mappingConfig.setGenerateModelsForRootTypes(MappingConfigConstants.DEFAULT_GENERATE_MODELS_FOR_ROOT_TYPES);
        }
        if (mappingConfig.getApiNamePrefixStrategy() == null) {
            mappingConfig.setApiNamePrefixStrategy(MappingConfigConstants.DEFAULT_API_NAME_PREFIX_STRATEGY);
        }
        if (mappingConfig.getApiRootInterfaceStrategy() == null) {
            mappingConfig.setApiRootInterfaceStrategy(MappingConfigConstants.DEFAULT_API_ROOT_INTERFACE_STRATEGY);
        }
        if (Boolean.TRUE.equals(mappingConfig.getGenerateClient())) {
            // required for request serialization
            mappingConfig.setGenerateToString(true);
        }
    }

    private static void validateConfigs(MappingConfig mappingConfig) {
        if (mappingConfig.getApiRootInterfaceStrategy() == ApiRootInterfaceStrategy.INTERFACE_PER_SCHEMA &&
                mappingConfig.getApiNamePrefixStrategy() == ApiNamePrefixStrategy.CONSTANT) {
            // we will have a conflict in case there is "type Query" in multiple graphql schema files
            throw new IllegalArgumentException("API prefix should not be CONSTANT for INTERFACE_PER_SCHEMA option");
        }
        if (mappingConfig.getGenerateApis() &&
                mappingConfig.getGenerateModelsForRootTypes() &&
                mappingConfig.getApiNamePrefixStrategy() == ApiNamePrefixStrategy.CONSTANT) {
            // checking for conflict between root type model classes and api interfaces
            if (Utils.stringsEqualIgnoreSpaces(mappingConfig.getApiNamePrefix(), mappingConfig.getModelNamePrefix()) &&
                    Utils.stringsEqualIgnoreSpaces(mappingConfig.getApiNameSuffix(), mappingConfig.getModelNameSuffix())) {
                // we will have a conflict between model pojo (Query.java) and api interface (Query.java)
                throw new IllegalArgumentException("Either disable APIs generation or set different Prefix/Suffix for API classes and model classes");
            }
            // checking for conflict between root type model resolver classes and api interfaces
            if (Utils.stringsEqualIgnoreSpaces(mappingConfig.getApiNamePrefix(), mappingConfig.getTypeResolverPrefix()) &&
                    Utils.stringsEqualIgnoreSpaces(mappingConfig.getApiNameSuffix(), mappingConfig.getTypeResolverSuffix())) {
                // we will have a conflict between model resolver interface (QueryResolver.java) and api interface resolver (QueryResolver.java)
                throw new IllegalArgumentException("Either disable APIs generation or set different Prefix/Suffix for API classes and type resolver classes");
            }
        }
    }

    private static void sanitizeValues(MappingConfig mappingConfig) {
        mappingConfig.setModelValidationAnnotation(
                Utils.replaceLeadingAtSign(mappingConfig.getModelValidationAnnotation()));

        Map<String, String> customAnnotationsMapping = mappingConfig.getCustomAnnotationsMapping();
        if (customAnnotationsMapping != null) {
            for (Map.Entry<String, String> entry : customAnnotationsMapping.entrySet()) {
                entry.setValue(Utils.replaceLeadingAtSign(entry.getValue()));
            }
        }
        Map<String, String> directiveAnnotationsMapping = mappingConfig.getDirectiveAnnotationsMapping();
        if (directiveAnnotationsMapping != null) {
            for (Map.Entry<String, String> entry : directiveAnnotationsMapping.entrySet()) {
                entry.setValue(Utils.replaceLeadingAtSign(entry.getValue()));
            }
        }
    }

    public List<File> generate() throws IOException {
        GraphQLCodegenFileCreator.prepareOutputDir(outputDir);
        long startTime = System.currentTimeMillis();
        List<File> generatedFiles = Collections.emptyList();
        if (!schemas.isEmpty()) {
            ExtendedDocument document = GraphQLDocumentParser.getDocument(mappingConfig, schemas);
            initCustomTypeMappings(document.getScalarDefinitions());
            generatedFiles = processDefinitions(document);
        }
        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println(String.format("Finished processing %d schema(s) in %d ms", schemas.size(), elapsed));
        return generatedFiles;
    }

    private List<File> processDefinitions(ExtendedDocument document) {
        MappingContext context = new MappingContext(mappingConfig, document, generatedInformation);

        List<File> generatedFiles = new ArrayList<>();
        for (ExtendedObjectTypeDefinition extendedObjectTypeDefinition : document.getTypeDefinitions()) {
            generatedFiles.addAll(generateType(context, extendedObjectTypeDefinition));
        }
        for (ExtendedObjectTypeDefinition extendedObjectTypeDefinition : document.getTypeDefinitions()) {
            generateFieldResolver(context, extendedObjectTypeDefinition.getFieldDefinitions(), extendedObjectTypeDefinition.getName())
                    .ifPresent(generatedFiles::add);
        }
        for (ExtendedObjectTypeDefinition extendedObjectTypeDefinition : document.getOperationDefinitions()) {
            if (Boolean.TRUE.equals(mappingConfig.getGenerateApis())) {
                generatedFiles.addAll(generateServerOperations(context, extendedObjectTypeDefinition));
            }
            if (Boolean.TRUE.equals(mappingConfig.getGenerateClient())) {
                generatedFiles.addAll(generateClient(context, extendedObjectTypeDefinition));
            }
        }
        for (ExtendedInputObjectTypeDefinition extendedInputObjectTypeDefinition : document.getInputDefinitions()) {
            generatedFiles.add(generateInput(context, extendedInputObjectTypeDefinition));
        }
        for (ExtendedEnumTypeDefinition extendedEnumTypeDefinition : document.getEnumDefinitions()) {
            generatedFiles.add(generateEnum(context, extendedEnumTypeDefinition));
        }
        for (ExtendedUnionTypeDefinition extendedUnionTypeDefinition : document.getUnionDefinitions()) {
            generatedFiles.addAll(generateUnion(context, extendedUnionTypeDefinition));
        }
        for (ExtendedInterfaceTypeDefinition extendedInterfaceTypeDefinition : document.getInterfaceDefinitions()) {
            generatedFiles.addAll(generateInterface(context, extendedInterfaceTypeDefinition));
        }
        for (ExtendedInterfaceTypeDefinition definition : document.getInterfaceDefinitions()) {
            generateFieldResolver(context, definition.getFieldDefinitions(), definition.getName())
                    .ifPresent(generatedFiles::add);
        }
        System.out.println(String.format("Generated %d definition classes in folder %s",
                generatedFiles.size(), outputDir.getAbsolutePath()));
        return generatedFiles;
    }

    private List<File> generateUnion(MappingContext mappingContext, ExtendedUnionTypeDefinition definition) {
        List<File> generatedFiles = new ArrayList<>();
        Map<String, Object> dataModel = UnionDefinitionToDataModelMapper.map(mappingContext, definition);
        generatedFiles.add(GraphQLCodegenFileCreator.generateFile(FreeMarkerTemplatesRegistry.unionTemplate, dataModel, outputDir));

        if (Boolean.TRUE.equals(mappingConfig.getGenerateClient())) {
            Map<String, Object> responseProjDataModel = RequestResponseDefinitionToDataModelMapper.mapResponseProjection(mappingContext, definition);
            generatedFiles.add(GraphQLCodegenFileCreator.generateFile(FreeMarkerTemplatesRegistry.responseProjectionTemplate, responseProjDataModel, outputDir));
        }
        return generatedFiles;
    }

    private List<File> generateInterface(MappingContext mappingContext, ExtendedInterfaceTypeDefinition definition) {
        List<File> generatedFiles = new ArrayList<>();
        Map<String, Object> dataModel = InterfaceDefinitionToDataModelMapper.map(mappingContext, definition);
        generatedFiles.add(GraphQLCodegenFileCreator.generateFile(FreeMarkerTemplatesRegistry.interfaceTemplate, dataModel, outputDir));

        if (Boolean.TRUE.equals(mappingConfig.getGenerateClient())) {
            Map<String, Object> responseProjDataModel = RequestResponseDefinitionToDataModelMapper.mapResponseProjection(mappingContext, definition);
            generatedFiles.add(GraphQLCodegenFileCreator.generateFile(FreeMarkerTemplatesRegistry.responseProjectionTemplate, responseProjDataModel, outputDir));
        }
        return generatedFiles;
    }

    private List<File> generateServerOperations(MappingContext mappingContext, ExtendedObjectTypeDefinition definition) {
        List<File> generatedFiles = new ArrayList<>();
        // Generate a root interface with all operations inside
        // Relates to https://github.com/facebook/relay/issues/112
        switch (mappingContext.getApiRootInterfaceStrategy()) {
            case INTERFACE_PER_SCHEMA:
                for (ExtendedObjectTypeDefinition defInFile : definition.groupBySourceLocationFile().values()) {
                    generatedFiles.add(generateRootApi(mappingContext, defInFile));
                }
                break;
            case SINGLE_INTERFACE:
            default:
                generatedFiles.add(generateRootApi(mappingContext, definition));
                break;
        }

        // Generate separate interfaces for all queries, mutations and subscriptions
        List<String> fieldNames = definition.getFieldDefinitions().stream().map(FieldDefinition::getName).collect(toList());
        switch (mappingContext.getApiNamePrefixStrategy()) {
            case FOLDER_NAME_AS_PREFIX:
                for (ExtendedObjectTypeDefinition fileDef : definition.groupBySourceLocationFolder().values()) {
                    generatedFiles.addAll(generateApis(mappingContext, fileDef, fieldNames));
                }
                break;
            case FILE_NAME_AS_PREFIX:
                for (ExtendedObjectTypeDefinition fileDef : definition.groupBySourceLocationFile().values()) {
                    generatedFiles.addAll(generateApis(mappingContext, fileDef, fieldNames));
                }
                break;
            case CONSTANT:
            default:
                generatedFiles.addAll(generateApis(mappingContext, definition, fieldNames));
                break;
        }
        return generatedFiles;
    }

    private List<File> generateClient(MappingContext mappingContext, ExtendedObjectTypeDefinition definition) {
        List<File> generatedFiles = new ArrayList<>();
        List<String> fieldNames = definition.getFieldDefinitions().stream().map(FieldDefinition::getName).collect(toList());
        for (ExtendedFieldDefinition operationDef : definition.getFieldDefinitions()) {
            Map<String, Object> requestDataModel = RequestResponseDefinitionToDataModelMapper.mapRequest(mappingContext, operationDef, definition.getName(), fieldNames);
            generatedFiles.add(GraphQLCodegenFileCreator.generateFile(FreeMarkerTemplatesRegistry.requestTemplate, requestDataModel, outputDir));

            Map<String, Object> responseDataModel = RequestResponseDefinitionToDataModelMapper.mapResponse(mappingContext, operationDef, definition.getName(), fieldNames);
            generatedFiles.add(GraphQLCodegenFileCreator.generateFile(FreeMarkerTemplatesRegistry.responseTemplate, responseDataModel, outputDir));
        }
        return generatedFiles;
    }

    private List<File> generateApis(MappingContext mappingContext, ExtendedObjectTypeDefinition definition, List<String> fieldNames) {
        List<File> generatedFiles = new ArrayList<>();
        for (ExtendedFieldDefinition operationDef : definition.getFieldDefinitions()) {
            Map<String, Object> dataModel = FieldDefinitionsToResolverDataModelMapper.mapRootTypeField(mappingContext, operationDef, definition.getName(), fieldNames);
            generatedFiles.add(GraphQLCodegenFileCreator.generateFile(FreeMarkerTemplatesRegistry.operationsTemplate, dataModel, outputDir));
        }
        return generatedFiles;
    }

    private File generateRootApi(MappingContext mappingContext, ExtendedObjectTypeDefinition definition) {
        Map<String, Object> dataModel = FieldDefinitionsToResolverDataModelMapper.mapRootTypeFields(mappingContext, definition);
        return GraphQLCodegenFileCreator.generateFile(FreeMarkerTemplatesRegistry.operationsTemplate, dataModel, outputDir);
    }

    private List<File> generateType(MappingContext mappingContext, ExtendedObjectTypeDefinition definition) {
        List<File> generatedFiles = new ArrayList<>();
        Map<String, Object> dataModel = TypeDefinitionToDataModelMapper.map(mappingContext, definition);
        generatedFiles.add(GraphQLCodegenFileCreator.generateFile(FreeMarkerTemplatesRegistry.typeTemplate, dataModel, outputDir));

        if (Boolean.TRUE.equals(mappingConfig.getGenerateClient())) {
            Map<String, Object> responseProjDataModel = RequestResponseDefinitionToDataModelMapper.mapResponseProjection(mappingContext, definition);
            generatedFiles.add(GraphQLCodegenFileCreator.generateFile(FreeMarkerTemplatesRegistry.responseProjectionTemplate, responseProjDataModel, outputDir));

            for (ExtendedFieldDefinition fieldDefinition : definition.getFieldDefinitions()) {
                if (!Utils.isEmpty(fieldDefinition.getInputValueDefinitions())) {
                    Map<String, Object> fieldProjDataModel = RequestResponseDefinitionToDataModelMapper.mapParametrizedInput(mappingContext, fieldDefinition, definition);
                    generatedFiles.add(GraphQLCodegenFileCreator.generateFile(FreeMarkerTemplatesRegistry.parametrizedInputTemplate, fieldProjDataModel, outputDir));
                }
            }
        }
        return generatedFiles;
    }

    private Optional<File> generateFieldResolver(MappingContext mappingContext, List<ExtendedFieldDefinition> fieldDefinitions, String definitionName) {
        List<ExtendedFieldDefinition> fieldDefsWithResolvers = fieldDefinitions.stream()
                .filter(fieldDef -> FieldDefinitionToParameterMapper.generateResolversForField(mappingContext, fieldDef, definitionName))
                .collect(toList());
        if (!fieldDefsWithResolvers.isEmpty()) {
            Map<String, Object> dataModel = FieldDefinitionsToResolverDataModelMapper.mapToTypeResolver(mappingContext, fieldDefsWithResolvers, definitionName);
            return Optional.of(GraphQLCodegenFileCreator.generateFile(FreeMarkerTemplatesRegistry.operationsTemplate, dataModel, outputDir));
        }
        return Optional.empty();
    }

    private File generateInput(MappingContext mappingContext, ExtendedInputObjectTypeDefinition definition) {
        Map<String, Object> dataModel = InputDefinitionToDataModelMapper.map(mappingContext, definition);
        return GraphQLCodegenFileCreator.generateFile(FreeMarkerTemplatesRegistry.typeTemplate, dataModel, outputDir);
    }

    private File generateEnum(MappingContext mappingContext, ExtendedEnumTypeDefinition definition) {
        Map<String, Object> dataModel = EnumDefinitionToDataModelMapper.map(mappingContext, definition);
        return GraphQLCodegenFileCreator.generateFile(FreeMarkerTemplatesRegistry.enumTemplate, dataModel, outputDir);
    }

    private void initCustomTypeMappings(Collection<ExtendedScalarTypeDefinition> scalarTypeDefinitions) {
        for (ExtendedScalarTypeDefinition definition : scalarTypeDefinitions) {
            if (definition.getDefinition() != null) {
                mappingConfig.putCustomTypeMappingIfAbsent(definition.getDefinition().getName(), "String");
            }
            for (ScalarTypeExtensionDefinition extension : definition.getExtensions()) {
                mappingConfig.putCustomTypeMappingIfAbsent(extension.getName(), "String");
            }
        }
        mappingConfig.putCustomTypeMappingIfAbsent("ID", "String");
        mappingConfig.putCustomTypeMappingIfAbsent("String", "String");
        mappingConfig.putCustomTypeMappingIfAbsent("Int", "Integer");
        mappingConfig.putCustomTypeMappingIfAbsent("Float", "Double");
        mappingConfig.putCustomTypeMappingIfAbsent("Boolean", "Boolean");
    }

}
