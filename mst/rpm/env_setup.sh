#!/bin/sh

function publish_sdk {
  ./gradlew :master-sdk:jarSdk :master-sdk:artifactoryPublish
}

function publish_policy_aar {
  ./gradlew :policy-sdk:assembleRelease :policy-sdk:artifactoryPublish
}

function publish_protobuf_param_lib {
  ./gradlew :param-types:protobuf-param:jarLib :param-types:protobuf-param:artifactoryPublish
}

function publish_protobuf_lite_param_lib {
  ./gradlew :param-types:protobuf-lite-param:jarLib :param-types:protobuf-lite-param:artifactoryPublish
}

function publish_gson_param_lib {
  ./gradlew :param-types:gson-param:jarLib :param-types:gson-param:artifactoryPublish
}
