import React from 'react';
import {Settings as SettingsIcon} from 'lucide-react';
import {Layout} from '@/components/Layout';
import {ResourceTypeSettings} from '@/components/settings/ResourceTypeSettings';

export const Settings: React.FC = () => {
  return (
      <Layout>
        <div className="container mx-auto px-4 py-8">
          <div className="mb-8">
            <div className="flex items-center gap-3 mb-2">
              <SettingsIcon className="h-8 w-8 text-icon-blue"/>
              <h1 className="text-3xl font-bold text-primary">Settings</h1>
            </div>
            <p className="text-secondary">
              Configure your AWS tag compliance platform settings
            </p>
          </div>

          <div className="max-w-4xl">
            <div className="bg-card rounded-lg shadow-md p-6">
              <ResourceTypeSettings/>
            </div>
          </div>
        </div>
      </Layout>
  );
};