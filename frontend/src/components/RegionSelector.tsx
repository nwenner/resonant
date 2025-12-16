import {useEffect, useState} from 'react';
import {Card, CardContent, CardDescription, CardHeader, CardTitle} from '@/components/ui/card';
import {Checkbox} from '@/components/ui/checkbox';
import {Button} from '@/components/ui/button';
import {Alert, AlertDescription} from '@/components/ui/alert';
import {Badge} from '@/components/ui/badge';
import {AlertCircle, CheckCircle2, Globe, RefreshCw} from 'lucide-react';
import type {AwsRegion} from '@/types/awsRegion';

interface RegionSelectorProps {
  regions: AwsRegion[];
  onSave: (enabledRegionCodes: string[]) => Promise<void>;
  onRediscover?: () => Promise<void>;
  isSaving: boolean;
  isRediscovering?: boolean;
}

export const RegionSelector = ({
                                 regions,
                                 onSave,
                                 onRediscover,
                                 isSaving,
                                 isRediscovering = false,
                               }: RegionSelectorProps) => {
  const [selectedRegions, setSelectedRegions] = useState<Set<string>>(new Set());
  const [hasChanges, setHasChanges] = useState(false);

  useEffect(() => {
    const enabled = new Set(regions.filter((r) => r.enabled).map((r) => r.regionCode));
    setSelectedRegions(enabled);
  }, [regions]);

  const handleToggle = (regionCode: string) => {
    setSelectedRegions((prev) => {
      const next = new Set(prev);
      if (next.has(regionCode)) {
        next.delete(regionCode);
      } else {
        next.add(regionCode);
      }
      return next;
    });
    setHasChanges(true);
  };

  const handleSelectAll = () => {
    setSelectedRegions(new Set(regions.map((r) => r.regionCode)));
    setHasChanges(true);
  };

  const handleDeselectAll = () => {
    setSelectedRegions(new Set());
    setHasChanges(true);
  };

  const handleSave = async () => {
    await onSave(Array.from(selectedRegions));
    setHasChanges(false);
  };

  const handleRediscover = async () => {
    if (onRediscover) {
      await onRediscover();
    }
  };

  const enabledCount = selectedRegions.size;
  const totalCount = regions.length;

  return (
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle className="flex items-center gap-2">
                <Globe className="h-5 w-5"/>
                AWS Regions
              </CardTitle>
              <CardDescription>
                Select which AWS regions to scan for resources
              </CardDescription>
            </div>
            {onRediscover && (
                <Button
                    variant="outline"
                    size="sm"
                    onClick={handleRediscover}
                    disabled={isRediscovering}
                >
                  <RefreshCw className={`h-4 w-4 mr-2 ${isRediscovering ? 'animate-spin' : ''}`}/>
                  Rediscover
                </Button>
            )}
          </div>
        </CardHeader>
        <CardContent className="space-y-4">
          {selectedRegions.size === 0 && (
              <Alert variant="destructive">
                <AlertCircle className="h-4 w-4"/>
                <AlertDescription>
                  At least one region must be enabled to perform scans
                </AlertDescription>
              </Alert>
          )}

          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Badge variant="secondary">
                {enabledCount} / {totalCount} enabled
              </Badge>
            </div>
            <div className="flex gap-2">
              <Button variant="ghost" size="sm" onClick={handleSelectAll}>
                Select All
              </Button>
              <Button variant="ghost" size="sm" onClick={handleDeselectAll}>
                Deselect All
              </Button>
            </div>
          </div>

          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-3">
            {[...regions].sort((a, b) => a.regionCode.localeCompare(b.regionCode)).map((region) => {
              const isSelected = selectedRegions.has(region.regionCode);
              return (
                  <div
                      key={region.id}
                      className={`
                  flex items-center space-x-2 p-3 rounded-md border cursor-pointer
                  transition-colors
                  ${isSelected
                          ? 'bg-primary/5 border-primary'
                          : 'bg-card border-border hover:bg-muted'
                      }
                `}
                      onClick={() => handleToggle(region.regionCode)}
                  >
                    <Checkbox
                        checked={isSelected}
                        className="pointer-events-none"
                    />
                    <span className="text-sm font-medium leading-none flex-1">
                  {region.regionCode}
                </span>
                  </div>
              );
            })}
          </div>

          {regions.length === 0 && (
              <div className="text-center py-8 text-muted-foreground">
                No regions discovered yet
              </div>
          )}

          <div className="flex items-center justify-between pt-4 border-t">
            {hasChanges && (
                <div className="flex items-center gap-2 text-sm text-muted-foreground">
                  <AlertCircle className="h-4 w-4"/>
                  Unsaved changes
                </div>
            )}
            {!hasChanges && enabledCount > 0 && (
                <div className="flex items-center gap-2 text-sm text-green-600 dark:text-green-400">
                  <CheckCircle2 className="h-4 w-4"/>
                  Settings saved
                </div>
            )}
            <div className="flex-1"/>
            <Button
                onClick={handleSave}
                disabled={isSaving || !hasChanges || selectedRegions.size === 0}
            >
              {isSaving ? 'Saving...' : 'Save Changes'}
            </Button>
          </div>
        </CardContent>
      </Card>
  );
};