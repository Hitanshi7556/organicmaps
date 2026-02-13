#include <jni.h>
#include "app/organicmaps/sdk/Framework.hpp"
#include "app/organicmaps/sdk/core/jni_helper.hpp"
#include "routing/router.hpp"
#include "routing/routing_options.hpp"

routing::VehicleType ToVehicleType(jint routerType)
{
  switch (static_cast<routing::RouterType>(routerType))
  {
  case routing::RouterType::Vehicle: return routing::VehicleType::Car;
  case routing::RouterType::Pedestrian:
  case routing::RouterType::Transit:
  case routing::RouterType::Ruler: return routing::VehicleType::Pedestrian;
  case routing::RouterType::Bicycle: return routing::VehicleType::Bicycle;
  case routing::RouterType::Count: break;
  }

  return routing::VehicleType::Car;
}

routing::RoutingOptions::Road makeValue(jint option)
{
  auto const road = static_cast<uint8_t>(1u << static_cast<int>(option));
  CHECK_LESS(road, static_cast<uint8_t>(routing::RoutingOptions::Road::Max), ());
  return static_cast<routing::RoutingOptions::Road>(road);
}

extern "C"
{
JNIEXPORT jboolean Java_app_organicmaps_sdk_routing_RoutingOptions_nativeHasOption(JNIEnv *, jclass, jint routerType, jint option)
{
  routing::RoutingOptions routingOptions = routing::RoutingOptions::LoadOptionsFromSettings(ToVehicleType(routerType));
  routing::RoutingOptions::Road road = makeValue(option);
  return static_cast<jboolean>(routingOptions.Has(road));
}

JNIEXPORT void Java_app_organicmaps_sdk_routing_RoutingOptions_nativeAddOption(JNIEnv *, jclass, jint routerType, jint option)
{
  routing::RoutingOptions routingOptions = routing::RoutingOptions::LoadOptionsFromSettings(ToVehicleType(routerType));
  routing::RoutingOptions::Road road = makeValue(option);
  routingOptions.Add(road);
  routing::RoutingOptions::SaveOptionsToSettings(ToVehicleType(routerType), routingOptions);
}

JNIEXPORT void Java_app_organicmaps_sdk_routing_RoutingOptions_nativeRemoveOption(JNIEnv *, jclass, jint routerType, jint option)
{
  routing::RoutingOptions routingOptions = routing::RoutingOptions::LoadOptionsFromSettings(ToVehicleType(routerType));
  routing::RoutingOptions::Road road = makeValue(option);
  routingOptions.Remove(road);
  routing::RoutingOptions::SaveOptionsToSettings(ToVehicleType(routerType), routingOptions);
}
}
