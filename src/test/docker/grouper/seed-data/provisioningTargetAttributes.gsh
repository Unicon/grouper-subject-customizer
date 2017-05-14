grouperSession = GrouperSession.startRootSession();

addRootStem("app", "app");
addGroup("app", "test", "test");

addStem("app", "SubjectSecurity", "SubjectSecurity");
addStem("app:SubjectSecurity", "groups", "groups");

addGroup("app:SubjectSecurity:groups", "privilegedGroups", "privilegedGroups");
addMember("app:SubjectSecurity:groups:privilegedGroups", "etc:sysadmingroup");

addGroup("app:SubjectSecurity:groups", "protectedGroups", "protectedGroups");
addMember("app:SubjectSecurity:groups:protectedGroups", "jgasper");

addGroup("app:SubjectSecurity:groups", "protectedGroups2", "protectedGroups2");
addMember("app:SubjectSecurity:groups:protectedGroups2", "alewis");
