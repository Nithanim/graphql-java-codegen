<#assign useOriginalName = true>

<#if package?has_content>
package ${package};

</#if>
<#if javaDoc?has_content>
/**
<#list javaDoc as javaDocLine>
 * ${javaDocLine}
</#list>
 */
</#if>
<#if generatedInfo.getGeneratedType()?has_content>
@${generatedInfo.getGeneratedType()}(
    value = "com.kobylynskyi.graphql.codegen.GraphQLCodegen",
    date = "${generatedInfo.getDateTime()}"
)
</#if>
<#list annotations as annotation>
@${annotation}
</#list>
public enum ${className}<#if implements?has_content> implements <#list implements as interface>${interface}<#if interface_has_next>, </#if></#list></#if> {

<#if fields?has_content>
<#list fields as field>
<#if field.javaDoc?has_content>
    /**
<#list field.javaDoc as javaDocLine>
     * ${javaDocLine}
</#list>
     */
</#if>
<#if field.deprecated>
    @Deprecated
</#if>
    ${field.javaName}<#if useOriginalName>("${field.graphqlName}")</#if><#if field_has_next>,<#else>;</#if>
</#list>
</#if>
<#if useOriginalName>
    private final String graphqlName;

    private ${className}(String graphqlName) {
        this.graphqlName = graphqlName;
    }

    @Override
    public String toString() {
        return this.graphqlName;
    }
</#if>
}