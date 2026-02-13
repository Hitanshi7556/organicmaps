#include "testing/testing.hpp"

#include "routing/routing_options.hpp"

#include <cstdint>
#include <vector>

using namespace routing;

namespace
{
using RoadType = RoutingOptions::RoadType;

class RoutingOptionsTests
{
public:
  RoutingOptionsTests()
    : m_savedCarOptions(RoutingOptions::LoadOptionsFromSettings(VehicleType::Car))
    , m_savedPedestrianOptions(RoutingOptions::LoadOptionsFromSettings(VehicleType::Pedestrian))
    , m_savedBicycleOptions(RoutingOptions::LoadOptionsFromSettings(VehicleType::Bicycle))
  {
  }

  ~RoutingOptionsTests()
  {
    RoutingOptions::SaveOptionsToSettings(VehicleType::Car, m_savedCarOptions);
    RoutingOptions::SaveOptionsToSettings(VehicleType::Pedestrian, m_savedPedestrianOptions);
    RoutingOptions::SaveOptionsToSettings(VehicleType::Bicycle, m_savedBicycleOptions);
  }

private:
  RoutingOptions m_savedCarOptions;
  RoutingOptions m_savedPedestrianOptions;
  RoutingOptions m_savedBicycleOptions;
};

RoutingOptions CreateOptions(std::vector<RoutingOptions::Road> const & include)
{
  RoutingOptions options;

  for (auto type : include)
    options.Add(type);

  return options;
}

void Checker(std::vector<RoutingOptions::Road> const & include)
{
  RoutingOptions options = CreateOptions(include);

  for (auto type : include)
    TEST(options.Has(type), ());

  auto max = static_cast<RoadType>(RoutingOptions::Road::Max);
  for (uint8_t i = 1; i < max; i <<= 1)
  {
    bool hasInclude = false;
    auto type = static_cast<RoutingOptions::Road>(i);
    for (auto has : include)
      hasInclude |= (type == has);

    if (!hasInclude)
      TEST(!options.Has(static_cast<RoutingOptions::Road>(i)), ());
  }
}

UNIT_TEST(RoutingOptionTest)
{
  Checker({RoutingOptions::Road::Toll, RoutingOptions::Road::Motorway, RoutingOptions::Road::Dirty});
  Checker({RoutingOptions::Road::Toll, RoutingOptions::Road::Dirty});

  Checker({RoutingOptions::Road::Toll, RoutingOptions::Road::Ferry, RoutingOptions::Road::Dirty});

  Checker({RoutingOptions::Road::Dirty});
  Checker({RoutingOptions::Road::Toll});
  Checker({RoutingOptions::Road::Dirty, RoutingOptions::Road::Motorway});
  Checker({});
}

UNIT_CLASS_TEST(RoutingOptionsTests, GetSetTest)
{
  RoutingOptions options =
      CreateOptions({RoutingOptions::Road::Toll, RoutingOptions::Road::Motorway, RoutingOptions::Road::Dirty});

  RoutingOptions::SaveCarOptionsToSettings(options);
  RoutingOptions fromSettings = RoutingOptions::LoadCarOptionsFromSettings();

  TEST_EQUAL(options.GetOptions(), fromSettings.GetOptions(), ());
}
UNIT_CLASS_TEST(RoutingOptionsTests, PedestrianAndBicycleRoundTrip)
{
  RoutingOptions const pedestrianOptions =
      CreateOptions({RoutingOptions::Road::Ferry, RoutingOptions::Road::Dirty});
  RoutingOptions const bicycleOptions =
      CreateOptions({RoutingOptions::Road::Motorway, RoutingOptions::Road::Dirty});

  RoutingOptions::SaveOptionsToSettings(VehicleType::Pedestrian, pedestrianOptions);
  RoutingOptions::SaveOptionsToSettings(VehicleType::Bicycle, bicycleOptions);

  TEST_EQUAL(RoutingOptions::LoadOptionsFromSettings(VehicleType::Pedestrian).GetOptions(),
             pedestrianOptions.GetOptions(), ());
  TEST_EQUAL(RoutingOptions::LoadOptionsFromSettings(VehicleType::Bicycle).GetOptions(),
             bicycleOptions.GetOptions(), ());
}

UNIT_CLASS_TEST(RoutingOptionsTests, SaveOneModeDoesNotAffectOthers)
{
  RoutingOptions const carOptions = CreateOptions({RoutingOptions::Road::Toll});
  RoutingOptions const pedestrianOptions = CreateOptions({RoutingOptions::Road::Ferry});
  RoutingOptions const bicycleOptions = CreateOptions({RoutingOptions::Road::Dirty});

  RoutingOptions::SaveOptionsToSettings(VehicleType::Car, carOptions);
  RoutingOptions::SaveOptionsToSettings(VehicleType::Pedestrian, pedestrianOptions);
  RoutingOptions::SaveOptionsToSettings(VehicleType::Bicycle, bicycleOptions);

  RoutingOptions::SaveOptionsToSettings(
      VehicleType::Pedestrian,
      CreateOptions({RoutingOptions::Road::Ferry, RoutingOptions::Road::Dirty}));

  TEST_EQUAL(RoutingOptions::LoadOptionsFromSettings(VehicleType::Car).GetOptions(),
             carOptions.GetOptions(), ());
  TEST_EQUAL(RoutingOptions::LoadOptionsFromSettings(VehicleType::Bicycle).GetOptions(),
             bicycleOptions.GetOptions(), ());
  TEST_EQUAL(RoutingOptions::LoadOptionsFromSettings(VehicleType::Pedestrian).GetOptions(),
             CreateOptions({RoutingOptions::Road::Ferry, RoutingOptions::Road::Dirty}).GetOptions(), ());
}

}  // namespace
