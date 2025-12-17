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
              <SettingsIcon className="h-8 w-8 text-blue-600 dark:text-blue-400"/>
              <h1 className="text-3xl font-bold text-gray-900 dark:text-white">Settings</h1>
            </div>
            <p className="text-gray-600 dark:text-gray-400">
              Configure your AWS tag compliance platform settings
            </p>
          </div>

          <div className="max-w-4xl">
            <div className="bg-white dark:bg-gray-800 rounded-lg shadow-md p-6">
              <ResourceTypeSettings/>
            </div>
          </div>
        </div>
      </Layout>
  );
};