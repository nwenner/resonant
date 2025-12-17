import React from 'react';
import {AlertCircle, CheckCircle, Cloud, Globe, Network} from 'lucide-react';
import {
  useResourceTypeSettings,
  useUpdateResourceTypeSetting
} from '@/hooks/useResourceTypeSettings';

const RESOURCE_TYPE_ICONS: Record<string, React.ComponentType<{ className?: string }>> = {
  's3:bucket': Cloud,
  'cloudfront:distribution': Globe,
  'ec2:vpc': Network,
};

export const ResourceTypeSettings: React.FC = () => {
  const {data: settings, isLoading, error} = useResourceTypeSettings();
  const updateMutation = useUpdateResourceTypeSetting();

  const handleToggle = async (resourceType: string, currentEnabled: boolean) => {
    try {
      await updateMutation.mutateAsync({
        resourceType,
        enabled: !currentEnabled,
      });
    } catch (error) {
      console.error('Failed to update resource type:', error);
    }
  };

  if (isLoading) {
    return (
        <div className="flex items-center justify-center py-8">
          <div
              className="animate-spin h-8 w-8 border-4 border-blue-600 border-t-transparent rounded-full"></div>
        </div>
    );
  }

  if (error) {
    return (
        <div className="error-box">
          <div className="flex items-center gap-2">
            <AlertCircle className="h-5 w-5"/>
            <span>Failed to load resource type settings</span>
          </div>
        </div>
    );
  }

  return (
      <div className="space-y-4">
        <div>
          <h3 className="text-lg font-semibold text-primary mb-2">
            Resource Types to Scan
          </h3>
          <p className="text-sm text-secondary">
            Enable or disable which AWS resource types are scanned for tag compliance.
          </p>
        </div>

        <div className="space-y-3">
          {settings?.map((setting) => {
            const Icon = RESOURCE_TYPE_ICONS[setting.resourceType] || Cloud;
            const isUpdating = updateMutation.isPending;

            return (
                <div
                    key={setting.id}
                    className="bg-card border border-default rounded-lg p-4 flex items-center justify-between"
                >
                  <div className="flex items-start gap-4 flex-1">
                    <div className="bg-blue-100 dark:bg-blue-900/30 p-3 rounded-lg">
                      <Icon className="h-6 w-6 text-icon-blue"/>
                    </div>
                    <div className="flex-1">
                      <div className="flex items-center gap-2">
                        <h4 className="font-medium text-primary">
                          {setting.displayName}
                        </h4>
                        {setting.enabled && (
                            <CheckCircle className="h-4 w-4 text-icon-green"/>
                        )}
                      </div>
                      <p className="text-sm text-secondary mt-1">
                        {setting.description}
                      </p>
                      <p className="text-xs text-secondary mt-1">
                        {setting.resourceType}
                      </p>
                    </div>
                  </div>

                  <button
                      onClick={() => handleToggle(setting.resourceType, setting.enabled)}
                      disabled={isUpdating}
                      className={`
                  relative inline-flex h-6 w-11 items-center rounded-full transition-colors
                  focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2
                  disabled:opacity-50 disabled:cursor-not-allowed
                  ${
                          setting.enabled
                              ? 'bg-blue-600'
                              : 'bg-gray-200 dark:bg-gray-700'
                      }
                `}
                  >
                <span
                    className={`
                    inline-block h-4 w-4 transform rounded-full bg-white transition-transform
                    ${setting.enabled ? 'translate-x-6' : 'translate-x-1'}
                  `}
                />
                  </button>
                </div>
            );
          })}
        </div>

        {settings?.length === 0 && (
            <div className="text-center py-8 text-secondary">
              No resource types configured
            </div>
        )}
      </div>
  );
};