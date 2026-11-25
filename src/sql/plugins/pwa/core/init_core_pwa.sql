-- liquibase formatted sql
-- changeset pwa:init_core_pwa.sql
-- preconditions onFail:MARK_RAN onError:WARN
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM core_admin_right WHERE id_right = 'PWA_MANAGEMENT'
INSERT INTO core_admin_right ( id_right, name, level_right, admin_url, description, is_updatable, plugin_name, id_feature_group, icon_url, documentation_url, id_order ) VALUES
( 'PWA_MANAGEMENT', 'pwa.adminFeature.managePwa.name', 2, 'jsp/admin/plugins/pwa/ManagePwa.jsp', 'pwa.adminFeature.managePwa.description', 0, 'pwa', 'SITE', 'ti ti-device-mobile-cog', NULL, 10 );

-- changeset pwa:init_core_pwa-rev1.sql
-- preconditions onFail:MARK_RAN onError:WARN
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM core_user_right WHERE id_right = 'PWA_MANAGEMENT' AND id_user = 1
INSERT INTO core_user_right ( id_right, id_user ) VALUES ( 'PWA_MANAGEMENT', 1 );
