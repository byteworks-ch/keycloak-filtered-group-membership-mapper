# Keycloak: Filtered Group Membership Mapper

A Keycloak Protocol Mapper that extends the built-in **Group Membership Mapper** by allowing regex-based exclusion of groups before they are included in tokens.

This is useful when you want to:
- Prevent specific groups from being exposed to a client
- Reduce token size by removing irrelevant groups

---

## ✨ Features

- 🔍 Regex-based **exclusion filtering** of group names
- 🎯 Works per client scope
- 🔐 Prevents sensitive or internal groups from appearing in tokens
- 🧩 Fully compatible with Keycloak Group Membership mapper
- ⚙️ Drop-in replacement for the default mapper

---

## 💡 Why this exists

Keycloak’s built-in **Group Membership mapper** always includes all groups assigned to a user.

In many real-world setups this becomes a problem:
- Users belong to many groups
- Some groups are internal and should not be exposed to clients
- Different clients should only see a subset of group memberships

Keycloak does not provide native support for filtering groups per client scope. This mapper solves that by allowing regex-based exclusion rules.

---

## 🚀 How it works

This mapper behaves like the standard Group Membership mapper, but applies a **regex exclusion filter** before adding groups to the token.

### Important behavior

- If a group **matches** the regex → ❌ it is removed
- If a group **does NOT match** → ✅ it is included

---

### Example

Given user groups:

```
/app1/user
/app1/admin
/app2/user
/app2/admin
/shared/auditors
```

With regex:

```
^/app1/.*$
```

Groups excluded from the token:

```
/app1/user
/app1/admin
```

Final token content:

```
/app2/user
/app2/admin
/shared/auditors
```

---

## ⚙️ Configuration

After deploying the provider JAR to Keycloak, configure it here:

```
Client → Client Scopes → Mappers → Create Mapper
```

### Mapper settings

| Setting | Description |
|--------|-------------|
| Mapper Type | Filtered Group Membership Mapper |
| Token Claim Name | Name of the claim (e.g. `groups`) |
| Full group path | Include full group path (recommended) |
| Regex Exclude Filter | Regex used to exclude matching groups |

---

## 🧠 Exclusion filter behavior

The regex is applied to each group path individually:

- Match → group is removed
- No match → group is included

If the regex is empty or not set, all groups are included (default behavior of Keycloak mapper).

---

## 🧪 Regex examples

| Goal | Regex (excluded groups) |
|------|-------------------------|
| Hide all `/app1` groups | `^/app1/.*$`            |
| Hide all admin groups | `.*admin.*`             |
| Hide shared groups | `^/shared/.*$`          |
| Hide multiple apps | `^/(app1\|app2)/.*$`    |
| Hide everything except app1 | `^(?!/app1/).*`         |

---

## 🔐 Security considerations

This mapper is useful in multi-tenant or microservice environments where:
- Group visibility must be restricted per client
- Internal group structure should not leak into tokens
- Tokens should remain minimal and relevant

⚠️ Important:
Regex misconfiguration can unintentionally expose or hide groups. Always validate patterns carefully.

---

## ⚠️ Limitations

- Only filters **group names (paths)**, not attributes or roles
- Does not modify Keycloak group assignments—only token output
- Requires careful regex design to avoid accidental exclusions
- Applies per token generation, not at user/group storage level

---

## 🛠 Compatibility

- Keycloak (Quarkus distribution recommended)
- OpenID Connect (OIDC)
- SAML (if using group mappers)

---

## 📦 Installation

### 1. Build the JAR

```bash
mvn clean package
```

### 2. Install the JAR

Copy the jar produced in target/ to your providers directory.

### 3. Restart Keycloak

Restart keycloak.
