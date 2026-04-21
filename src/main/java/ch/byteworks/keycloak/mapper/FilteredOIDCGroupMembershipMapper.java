package ch.byteworks.keycloak.mapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

import org.keycloak.models.*;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.*;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;

public class FilteredOIDCGroupMembershipMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper, TokenIntrospectionTokenMapper {
    public static final String CONF_FULL_PATH = "full.path";
    public static final String CONF_EXCLUDE_PATTERN = "exclude.pattern";

    public static final String PROVIDER_ID = "oidc-filtered-group-membership-mapper";

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    static {
        ProviderConfigProperty property;

        property = new ProviderConfigProperty();
        property.setName(CONF_FULL_PATH);
        property.setLabel("Full group path");
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property.setDefaultValue("true");
        property.setHelpText("Include full path to group i.e. /top/level1/level2, false will just specify the group name");
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(CONF_EXCLUDE_PATTERN);
        property.setLabel("Exclude Groups (Regex)");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setHelpText("Regex pattern for groups to exclude");
        configProperties.add(property);

        OIDCAttributeMapperHelper.addIncludeInTokensConfig(configProperties, GroupMembershipMapper.class);
        OIDCAttributeMapperHelper.addTokenClaimNameConfig(configProperties);
    }

    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Filtered Group Membership";
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Map user group membership with filtering support.";
    }

    public static boolean useFullPath(ProtocolMapperModel mappingModel) {
        return "true".equals(mappingModel.getConfig().get(CONF_FULL_PATH));
    }

    protected void setClaim(
            IDToken token,
            ProtocolMapperModel mappingModel,
            UserSessionModel userSession,
            KeycloakSession keycloakSession,
            ClientSessionContext clientSessionCtx) {
        Function<GroupModel, String> toGroupRepresentation = useFullPath(mappingModel) ?
                ModelToRepresentation::buildGroupPath : GroupModel::getName;

        String regex = mappingModel.getConfig().get(CONF_EXCLUDE_PATTERN);
        final Pattern excludePattern = (regex == null || regex.isEmpty()) ? null : Pattern.compile(regex);

        List<String> membership = userSession.getUser().getGroupsStream()
                .filter(group -> {
                    if (excludePattern == null) return true;
                    String groupPath = ModelToRepresentation.buildGroupPath(group);
                    return !excludePattern.matcher(groupPath).find();
                })
                .map(toGroupRepresentation)
                .collect(Collectors.toList());

        // Force attribute as multivalued, as it is not defined for this mapper
        mappingModel.getConfig().put(ProtocolMapperUtils.MULTIVALUED, "true");
        OIDCAttributeMapperHelper.mapClaim(token, mappingModel, membership);
    }

    public static ProtocolMapperModel create(String name,
                                             String tokenClaimName,
                                             boolean consentRequired, String consentText,
                                             boolean accessToken, boolean idToken, boolean introspectionEndpoint) {
        ProtocolMapperModel mapper = new ProtocolMapperModel();
        mapper.setName(name);
        mapper.setProtocolMapper(PROVIDER_ID);
        mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);

        Map<String, String> config = new HashMap<String, String>();

        config.put(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, tokenClaimName);
        if (accessToken) config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");
        if (idToken) config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "true");
        if (introspectionEndpoint) config.put(OIDCAttributeMapperHelper.INCLUDE_IN_INTROSPECTION, "true");

        mapper.setConfig(config);

        return mapper;
    }
}
